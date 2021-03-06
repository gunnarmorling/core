/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.injection.producer.ejb;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler;
import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.DynamicInjectionPoint;
import org.jboss.weld.injection.InjectionContextImpl;
import org.jboss.weld.injection.producer.AbstractInjectionTarget;
import org.jboss.weld.injection.producer.DefaultInstantiator;
import org.jboss.weld.injection.producer.Instantiator;
import org.jboss.weld.injection.producer.SubclassDecoratorApplyingInstantiator;
import org.jboss.weld.injection.producer.SubclassedComponentInstantiator;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.Types;

public class SessionBeanInjectionTarget<T> extends AbstractInjectionTarget<T> {

    private final SessionBean<T> bean;
    private final CurrentInjectionPoint currentInjectionPoint;

    public SessionBeanInjectionTarget(EnhancedAnnotatedType<T> type, SessionBean<T> bean, BeanManagerImpl beanManager) {
        super(type, bean, beanManager);
        this.bean = bean;
        this.currentInjectionPoint = beanManager.getServices().get(CurrentInjectionPoint.class);
    }

    @Override
    public SessionBean<T> getBean() {
        return bean;
    }

    @Override
    public void inject(final T instance, final CreationalContext<T> ctx) {
        new InjectionContextImpl<T>(getBeanManager(), this, getType(), instance) {

            public void proceed() {
                if (isStatelessSessionBean()) {
                    currentInjectionPoint.push(new DynamicInjectionPoint(beanManager.getServices()));
                    try {
                        injectFieldsAndInitializers();
                    } finally {
                        currentInjectionPoint.pop();
                    }
                } else {
                    injectFieldsAndInitializers();
                }
            }

            private boolean isStatelessSessionBean() {
                return getBean().getEjbDescriptor().isStateless();
            }

            private void injectFieldsAndInitializers() {
                Beans.injectFieldsAndInitializers(instance, ctx, getBeanManager(), getInjectableFields(), getInitializerMethods());
            }

        }.run();
    }

    @Override
    protected Instantiator<T> initInstantiator(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager, Set<InjectionPoint> injectionPoints) {
        if (bean instanceof SessionBean<?>) {
            DefaultInstantiator<T> instantiator = new DefaultInstantiator<T>(type, bean, beanManager);
            injectionPoints.addAll(instantiator.getConstructor().getParameterInjectionPoints());
            return instantiator;
        } else {
            throw new IllegalArgumentException("Cannot create SessionBeanInjectionTarget for " + bean);
        }
    }

    @Override
    protected void initializeAfterBeanDiscovery(EnhancedAnnotatedType<T> annotatedType) {
        super.initializeAfterBeanDiscovery(annotatedType);
        List<Decorator<?>> decorators = beanManager.resolveDecorators(getBean().getTypes(), getBean().getQualifiers());
        if (!decorators.isEmpty()) {
            Instantiator<T> instantiator = getInstantiator();
            instantiator = new SubclassedComponentInstantiator<T>(annotatedType, getBean(),
                    (DefaultInstantiator<T>) instantiator, beanManager);
            instantiator = new SubclassDecoratorApplyingInstantiator<T>(instantiator, getBean(), decorators);
            setInstantiator(instantiator);
        }
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        T result = super.produce(ctx);

        if (result instanceof ProxyObject) {
            // if decorators are applied, use SessionBeanViewMethodHandler
            ProxyObject proxy = (ProxyObject) result;
            proxy.setHandler(new SessionBeanViewMethodHandler(bean.getTypes(), (CombinedInterceptorAndDecoratorStackMethodHandler) proxy.getHandler()));
        }
        return result;
    }

    /**
     * This {@link MethodHandler} assures that any method invoked on a decorated {@link SessionBean} is a method that belongs to
     * a bean type of the bean. If the methods belongs to the bean implementation and the bean implementation is not a bean type
     * the handler tries to replace the method with the equivalent method defined on a bean type.
     *
     * @author Jozef Hartinger
     *
     */
    private static class SessionBeanViewMethodHandler extends CombinedInterceptorAndDecoratorStackMethodHandler {

        private static final long serialVersionUID = -8038819529432133787L;

        private final Set<Class<?>> beanTypes;

        public SessionBeanViewMethodHandler(Set<Type> types, CombinedInterceptorAndDecoratorStackMethodHandler delegate) {
            this.beanTypes = Types.getRawTypes(types);
            setOuterDecorator(delegate.getOuterDecorator());
            setInterceptorMethodHandler(delegate.getInterceptorMethodHandler());
        }

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            if (beanTypes.contains(thisMethod.getDeclaringClass())) {
                return super.invoke(self, thisMethod, proceed, args);
            } else {
                Method decoratedTypeMethod = getBeanTypeMethod(thisMethod);
                return super.invoke(self, decoratedTypeMethod, proceed, args);
            }
        }

        private Method getBeanTypeMethod(Method method) {
            for (Class<?> c : beanTypes) {
                try {
                    return c.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    continue;
                }
            }
            return method;
        }
    }
}
