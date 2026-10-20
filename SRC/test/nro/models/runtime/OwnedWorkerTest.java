package nro.models.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class OwnedWorkerTest {
    public static void main(String[] args) throws Exception {
        Class<?> type;
        try { type = Class.forName("nro.models.utils.OwnedWorker"); }
        catch (ClassNotFoundException e) { throw new AssertionError("owned worker lifecycle is missing"); }
        Object worker = type.getConstructor().newInstance();
        var start = type.getMethod("start", String.class, Runnable.class, java.util.function.BooleanSupplier.class);
        AtomicBoolean open = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        Runnable body = () -> { calls.incrementAndGet(); entered.countDown(); try { release.await(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } finally { done.countDown(); }};
        start.invoke(worker,"fixture-owner",body,(java.util.function.BooleanSupplier)open::get);
        if(!entered.await(2,TimeUnit.SECONDS))throw new AssertionError("worker did not start");
        for(int i=0;i<20;i++)start.invoke(worker,"fixture-owner",body,(java.util.function.BooleanSupplier)open::get);
        if(calls.get()!=1)throw new AssertionError("duplicate start created multiple workers");
        open.set(false); release.countDown();
        if(!done.await(2,TimeUnit.SECONDS))throw new AssertionError("worker did not end");
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(Thread.getAllStackTraces().keySet().stream().anyMatch(t->t.getName().equals("fixture-owner")) && System.nanoTime()<until)Thread.yield();
        if(Thread.getAllStackTraces().keySet().stream().anyMatch(t->t.getName().equals("fixture-owner")))throw new AssertionError("idle executor thread leaked");
        Object interruptedWorker = type.getConstructor().newInstance();
        AtomicBoolean interruptedOpen = new AtomicBoolean(true);
        AtomicInteger interruptedCalls = new AtomicInteger();
        CountDownLatch restarted = new CountDownLatch(1);
        Runnable interruptedBody = () -> {
            if(interruptedCalls.incrementAndGet()>1) restarted.countDown();
            Thread.currentThread().interrupt();
        };
        start.invoke(interruptedWorker,"fixture-interrupt",interruptedBody,(java.util.function.BooleanSupplier)interruptedOpen::get);
        boolean duplicate = restarted.await(300,TimeUnit.MILLISECONDS);
        interruptedOpen.set(false);
        if(duplicate)throw new AssertionError("interruption must end worker without automatic restart");
        System.out.println("OWNED_WORKER PASS");
    }
}
