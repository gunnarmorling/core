/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.interceptor.proxy;

import static org.jboss.weld.util.collections.WeldCollections.immutableMap;
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.reader.cache.MetadataCachingReader;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Holds interceptor metadata and interceptor instances throughout the lifecycle of the intercepted instance.
 *
 * @author Jozef Hartinger
 *
 */
public class InterceptionContext implements Serializable {

    private static final long serialVersionUID = 7500722360133273633L;

    private final transient TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata;
    private final transient InterceptionModel<ClassMetadata<?>, ?> interceptionModel;

    private final Map<Class<?>, Object> interceptorInstances;
    private final BeanManagerImpl manager;
    private final Class<?> targetClass;

    public InterceptionContext(TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata, InterceptionModel<ClassMetadata<?>, ?> interceptionModel, CreationalContext<?> ctx, BeanManagerImpl manager) {
        this(initInterceptorInstanceMap(interceptionModel.getAllInterceptors(), ctx, manager), manager, targetClassInterceptorMetadata, interceptionModel, targetClassInterceptorMetadata.getInterceptorClass().getJavaClass());
    }

    private InterceptionContext(Map<Class<?>, Object> interceptorInstances, BeanManagerImpl manager, TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata, InterceptionModel<ClassMetadata<?>, ?> interceptionModel, Class<?> targetClass) {
        this.interceptorInstances = interceptorInstances;
        this.manager = manager;
        this.targetClassInterceptorMetadata = targetClassInterceptorMetadata;
        this.interceptionModel = interceptionModel;
        this.targetClass = targetClass;
    }

    private static Map<Class<?>, Object> initInterceptorInstanceMap(Iterable<? extends InterceptorMetadata<?>> interceptorMetadata, CreationalContext ctx, BeanManagerImpl manager) {
        Map<Class<?>, Object> interceptorInstances = new HashMap<Class<?>, Object>();
        for (InterceptorMetadata<?> interceptor : interceptorMetadata) {
            interceptorInstances.put(interceptor.getInterceptorClass().getJavaClass(), interceptor.getInterceptorFactory().create(ctx, manager));
        }
        return immutableMap(interceptorInstances);
    }

    public TargetClassInterceptorMetadata<?> getTargetClassInterceptorMetadata() {
        return targetClassInterceptorMetadata;
    }

    public InterceptionModel<ClassMetadata<?>, ?> getInterceptionModel() {
        return interceptionModel;
    }

    public <T> T getInterceptorInstance(InterceptorMetadata<T> interceptorMetadata) {
        return cast(interceptorInstances.get(interceptorMetadata.getInterceptorClass().getJavaClass()));
    }

    private Object readResolve() throws ObjectStreamException {
        InterceptionModel<ClassMetadata<?>, ?> interceptionModel = manager.getInterceptorModelRegistry().get(targetClass);
        MetadataCachingReader reader = manager.getInterceptorMetadataReader();
        TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata = reader.getTargetClassInterceptorMetadata(reader.getClassMetadata(targetClass));
        return new InterceptionContext(interceptorInstances, manager, targetClassInterceptorMetadata, interceptionModel, targetClass);
    }
}
