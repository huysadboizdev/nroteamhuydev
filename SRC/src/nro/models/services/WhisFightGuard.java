package nro.models.services;

import java.util.concurrent.ConcurrentHashMap;

/** One pending or live Whis per owner, including both TrainingService routes. */
public final class WhisFightGuard {
    private static final ConcurrentHashMap<Long,Object> FIGHTS = new ConcurrentHashMap<>();
    private WhisFightGuard() {}
    public static Object reserve(long ownerId) {
        Object token = new Object();
        return FIGHTS.putIfAbsent(ownerId,token)==null ? token : null;
    }
    public static void release(long ownerId,Object token) {
        if(token!=null) FIGHTS.remove(ownerId,token);
    }
}
