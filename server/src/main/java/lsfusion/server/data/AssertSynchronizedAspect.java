package lsfusion.server.data;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.WeakIdentityHashMap;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.server.ServerLoggers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.server.ServerLoggers.sqlLogger;

@Aspect
public class AssertSynchronizedAspect {

    private static Map<Object, WeakReference<Thread>> map = new ConcurrentHashMap<Object, WeakReference<Thread>>();

    @Around("execution(@lsfusion.server.data.AssertSynchronized * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        
        Thread currentThread = Thread.currentThread();
        WeakReference<Thread> prevWeakThread = map.put(object, new WeakReference<Thread>(currentThread));
        if(prevWeakThread != null) { // работает не максимально надежно, но смысл в том что сам exception и так время от времени будет появляться
            Thread prevThread = prevWeakThread.get();
            ServerLoggers.assertLog(false, "ASSERT SYNCHRONIZED " + '\n' + ExceptionUtils.getStackTrace(currentThread.getStackTrace()) + '\n' + 
                    (prevThread == null? "DEAD" : ExceptionUtils.getStackTrace(prevThread.getStackTrace())));
        }
        
        try {
            return thisJoinPoint.proceed();
        } finally {
            map.remove(object);
        }
    }

}
