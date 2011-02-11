package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.*;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.AbstractSourceJoin;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Aspect
public class CacheAspect {

/*    public static interface ImmutableInterface {
        Map getCaches();
    }
    public static class ImmutableInterfaceImplement implements ImmutableInterface {
        public ImmutableInterfaceImplement() {
        }

        private Map caches = null;
        public Map getCaches() {
            if(caches==null) caches = new HashMap();
            return caches;
        }
    }

    @DeclareParents(value="@net.jcip.annotations.Immutable *",defaultImpl=ImmutableInterfaceImplement.class)
    private ImmutableInterface immutable;
  */

    static class Invocation extends TwinImmutableObject {
        final Method method;
        final Object[] args;

        Invocation(ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return method +"(" + Arrays.asList(args) + ')';
        }

        public boolean twins(TwinImmutableInterface o) {
            return method.equals(((Invocation)o).method) && Arrays.equals(args,((Invocation)o).args);
        }

        public int immutableHashCode() {
            return 31* method.hashCode() + Arrays.hashCode(args);
        }
    }

    private Object lazyExecute(ImmutableObject object,ProceedingJoinPoint thisJoinPoint,Object[] args) throws Throwable {
        Invocation invoke = new Invocation(thisJoinPoint,args);
        Map caches = object.getCaches();
        Object result = caches.get(invoke);
        if(result==null) {
            result = thisJoinPoint.proceed();
            caches.put(invoke,result);
        }
        return result;
    }

    static class IdentityInvocation {
        final WeakReference<Object> targetRef;

        final Method method;
        final Object[] args;

        public IdentityInvocation(ReferenceQueue<Object> refQueue, Object target, ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.targetRef = new IdentityInvocationWeakMap.InvocationWeakReference(target, refQueue, this);
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return method + "(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            IdentityInvocation invocation = (IdentityInvocation) o;
            if (invocation == null) return false;
            Object thisTarget = targetRef.get();
            Object otherTarget = invocation.targetRef.get();
            return thisTarget != null && thisTarget == otherTarget && method.equals(invocation.method) && Arrays.equals(args, invocation.args);
        }

        @Override
        public int hashCode() {
            Object thisTarget = targetRef.get();
            if (thisTarget != null) {
                return 31 * (31 * System.identityHashCode(thisTarget) + method.hashCode()) + Arrays.hashCode(args);
            } else {
                return 31 * method.hashCode() + Arrays.hashCode(args);
            }
        }
    }

    static class IdentityInvocationWeakMap {
        private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();

        private Map<IdentityInvocation, Object> map = new HashMap<IdentityInvocation, Object>();

        public Object get(IdentityInvocation key) {
            expunge();
            return map.get(key);
        }

        public Object put(IdentityInvocation key, Object value) {
            expunge();
            return map.put(key, value);
        }

        public Object remove(IdentityInvocation key) {
            expunge();
            return map.remove(key);
        }

        public int size() {
            expunge();
            return map.size();
        }

        public ReferenceQueue<Object> getRefQueue() {
            return refQueue;
        }

        private void expunge() {
            InvocationWeakReference ref;
            while ((ref = (InvocationWeakReference) refQueue.poll()) != null) {
                map.remove(ref.invocation);
                ref.invocation = null;
            }
        }

        public static class InvocationWeakReference extends WeakReference {
            IdentityInvocation invocation;
            public InvocationWeakReference(Object target, ReferenceQueue<Object> refQueue, IdentityInvocation invocation) {
                super(target, refQueue);
                this.invocation = invocation;
            }
        }
    }

//    public final static SoftHashMap<IdentityInvocation, Object> lazyIdentityExecute = new SoftHashMap<IdentityInvocation, Object>();
    public final static IdentityInvocationWeakMap lazyIdentityExecute = new IdentityInvocationWeakMap();

    private Object lazyIdentityExecute(Object target, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        if(args.length>0 && args[0] instanceof NoCacheInterface)
            return thisJoinPoint.proceed();
        
        if(target instanceof ImmutableObject)
            return lazyExecute((ImmutableObject) target, thisJoinPoint, args);

        IdentityInvocation invocation = new IdentityInvocation(lazyIdentityExecute.getRefQueue(), target, thisJoinPoint, args);
        Object result = lazyIdentityExecute.get(invocation);
        if (result == null) {
            result = thisJoinPoint.proceed();
            lazyIdentityExecute.put(invocation, result);
        }

        return result;
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@platform.server.caches.IdentityLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyIdentityExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }

    //@net.jcip.annotations.Immutable *
    @Around("execution(@platform.server.caches.ParamLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callParamMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyIdentityExecute(args[0], thisJoinPoint, switchArgs);
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@platform.server.caches.SynchronizedLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callSynchronizedMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        synchronized (object) {
            return callMethod(thisJoinPoint, object);
        }
    }

    static class TwinInvocation {
        final Object object;
        final Method method;
        final Object[] args;

        TwinInvocation(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.object = object;
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return object + "." + method + "(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            return o != null && object.equals(((TwinInvocation) o).object) && method.equals(((TwinInvocation) o).method) && Arrays.equals(args, ((TwinInvocation) o).args);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * object.hashCode() + method.hashCode()) + Arrays.hashCode(args);
        }
    }

    public final static SoftHashMap<TwinInvocation, Object> lazyTwinExecute = new SoftHashMap<TwinInvocation, Object>();

    private Object lazyTwinExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        TwinInvocation invoke = new TwinInvocation(object, thisJoinPoint, args);
        Object result = lazyTwinExecute.get(invoke);
        if (result == null) {
            result = thisJoinPoint.proceed();
            lazyTwinExecute.put(invoke, result);
        }

        return result;
    }

    @Around("execution(@platform.server.caches.TwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyTwinExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }

    public final static SoftHashMap<Object, Object> lazyTwinManualExecute = new SoftHashMap<Object, Object>();
    private Object lazyTwinManualExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        Object twin = lazyTwinManualExecute.get(object);
        if(twin==null) {
            twin = object;
            lazyTwinManualExecute.put(object, object);
        }
        if (twin == object)
            return thisJoinPoint.proceed();
        else // нужно вызвать тот же метод но twin объекта
            return thisJoinPoint.proceed(BaseUtils.add(new Object[]{twin}, args));
    }
    @Around("execution(@platform.server.caches.TwinManualLazy * *.*(..)) && target(object)")
    public Object callTwinManualMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyTwinManualExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }
    
    @Around("execution(@platform.server.caches.ParamTwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callParamTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyTwinExecute(args[0], thisJoinPoint, switchArgs);
    }

    public static class TwinsCall {
        Object twin1;
        Object twin2;

        TwinsCall(Object twin1, Object twin2) {
            this.twin1 = twin1;
            this.twin2 = twin2;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof TwinsCall &&
                                ((twin1 == ((TwinsCall) o).twin1 && twin2 == ((TwinsCall) o).twin2) ||
                                 (twin2 == ((TwinsCall) o).twin1 && twin1 == ((TwinsCall) o).twin2));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(twin1) + System.identityHashCode(twin2);
        }
    }

    static class Twins {
        WeakIdentityHashSet<Object> objects;
        HashSet<Twins> diff;

        Twins(Object object) {
            objects = new WeakIdentityHashSet<Object>(object);
            diff = new HashSet<Twins>();
        }
    }

    static class TwinsMap extends WeakIdentityHashMap<Object, Twins> {

        Twins getTwins(Object object) {
            Twins twins = get(object);
            if (twins == null) {
                twins = new Twins(object);
                put(object, twins);
            }
            return twins;
        }

        public boolean call(ProceedingJoinPoint thisJoinPoint, Object object1, Object object2) throws Throwable {
            Twins twins1 = getTwins(object1);
            Twins twins2 = getTwins(object2);

            if (twins1.equals(twins2))
                return true;

            if (twins1.diff.contains(twins2))
                return false;

            if ((Boolean) thisJoinPoint.proceed()) {
                // "перекидываем" все компоненты в первую
                for (Object object : twins2.objects)
                    put(object, twins1);
                // сливаем компоненты, сами objects и diff
                twins1.objects.addAll(twins2.objects);
                // сливаем diff'ы
                twins1.diff.addAll(twins2.diff);
                for (Twins eqd : twins2.diff) { // заменяем equal2 на equal1
                    eqd.diff.remove(twins2);
                    eqd.diff.add(twins1);
                }
                return true;
            } else {
//                assert false;
                
                twins1.diff.add(twins2);
                twins2.diff.add(twins1);
                return false;
            }
        }
    }

    public static TwinsMap cacheTwins = new TwinsMap();

    @Around("execution(boolean platform.base.TwinImmutableInterface.twins(platform.base.TwinImmutableInterface)) && target(object) && args(twin)")
    // с call'ом есть баги
    public Object callTwins(ProceedingJoinPoint thisJoinPoint, GroupExpr object, AbstractSourceJoin twin) throws Throwable {
        return cacheTwins.call(thisJoinPoint, object, twin);
    }
}
