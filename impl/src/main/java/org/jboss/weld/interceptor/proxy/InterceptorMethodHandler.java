package org.jboss.weld.interceptor.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.interceptor.spi.context.InvocationContextFactory;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.interceptor.util.InterceptionUtils;
import org.jboss.weld.util.reflection.SecureReflections;

/**
 * @author Marius Bogoevici
 * @author Marko Luksa
 * @author Jozef Hartinger
 */
public class InterceptorMethodHandler implements MethodHandler, Serializable {

    private final InterceptionContext ctx;
    private final InvocationContextFactory factory;

    public InterceptorMethodHandler(InterceptionContext ctx, InvocationContextFactory factory) {
        this.ctx = ctx;
        this.factory = factory;
    }

    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        SecureReflections.ensureAccessible(thisMethod);
        if (proceed == null) {
            if (thisMethod.getName().equals(InterceptionUtils.POST_CONSTRUCT)) {
                return executeInterception(self, null, null, InterceptionType.POST_CONSTRUCT);
            } else if (thisMethod.getName().equals(InterceptionUtils.PRE_DESTROY)) {
                return executeInterception(self, null, null, InterceptionType.PRE_DESTROY);
            }
        } else {
            if (isInterceptorMethod(thisMethod)) {
                return proceed.invoke(self, args);
            }
            return executeInterception(self, thisMethod, args, InterceptionType.AROUND_INVOKE);
        }
        return null;
    }

    protected Object executeInterception(Object instance, Method method, Object[] args, InterceptionType interceptionType) throws Throwable {
        SimpleInterceptionChain chain = new SimpleInterceptionChain(instance, method, args, interceptionType, ctx);
        return chain.invokeNextInterceptor(factory.newInvocationContext(chain, instance, method, args));
    }

    private boolean isInterceptorMethod(Method method) {
        MethodMetadata methodMetadata = ctx.getTargetClassInterceptorMetadata().getInterceptorClass().getDeclaredMethod(method);
        return methodMetadata != null && methodMetadata.isInterceptorMethod();
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        try {
            executeInterception(null, null, null, InterceptionType.PRE_PASSIVATE);
            objectOutputStream.defaultWriteObject();
        } catch (Throwable throwable) {
            throw new IOException("Error while serializing class", throwable);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            objectInputStream.defaultReadObject();
            executeInterception(null, null, null, InterceptionType.POST_ACTIVATE);
        } catch (Throwable throwable) {
            throw new IOException("Error while deserializing class", throwable);
        }
    }

}
