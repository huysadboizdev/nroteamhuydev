package nro.models.services;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import static nro.models.services.ServerMaintenanceService.*;

/** Deterministic coordinator tests; no server/SQL side effects. */
public final class ServerMaintenanceTest {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    static final class MemoryStore implements Store {
        State state = new State("OPEN",0,0,"");
        Intent pending;
        boolean lostCommit, failTerminal;
        int accepted, heartbeats;
        public State load(String run) { return state; }
        public void heartbeat(String run) { heartbeats++; }
        public Intent next() { return pending; }
        public State accept(Intent intent, String run) throws Exception {
            pending=null; accepted++;
            state=new State("MAINTENANCE_START".equals(intent.type())?"DRAINING":"OPEN",state.revision()+1,intent.id(),"");
            if (lostCommit) { lostCommit=false; throw new java.sql.SQLException("Lost commit acknowledgement"); }
            return state;
        }
        public State finish(State expected,String phase,String message,String run) throws Exception {
            if(state.revision()!=expected.revision()) return state;
            state=new State(phase,state.revision()+1,state.commandId(),message);
            if(failTerminal) { failTerminal=false; throw new java.sql.SQLException("Lost terminal acknowledgement"); }
            return state;
        }
    }
    public static void main(String[] args) throws Exception {
        MemoryStore store=new MemoryStore(); int[] saves={0};
        ServerMaintenanceService service=new ServerMaintenanceService(store,()->{
            check(ServerMaintenanceGate.paused(),"admission closed before save");
            check("DRAINING".equals(store.state.phase()),"durable intent before effects");
            saves[0]++;
        });
        service.initialize(); check(!ServerMaintenanceGate.paused(),"open boot");
        store.pending=new Intent(1,"MAINTENANCE_START");service.tick();
        check(saves[0]==1&&"MAINTENANCE".equals(store.state.phase()),"drain and complete");
        service.tick();check(saves[0]==1,"terminal state never redrains");
        store.pending=new Intent(2,"MAINTENANCE_OPEN");service.tick();
        check(!ServerMaintenanceGate.paused(),"open after completed maintenance");
        store.lostCommit=true;store.pending=new Intent(3,"MAINTENANCE_START");
        try { service.tick();throw new AssertionError("expected lost acknowledgement"); } catch(java.sql.SQLException expected) {}
        check(ServerMaintenanceGate.paused()&&saves[0]==1,"uncertain start remains closed without speculative save");
        service.tick();check(saves[0]==2&&"MAINTENANCE".equals(store.state.phase()),"recover committed start");
        store.state=new State("DRAINING",8,4,"");
        ServerMaintenanceService failed=new ServerMaintenanceService(store,()->{throw new IllegalStateException("SQL save failed");});
        try { failed.tick();throw new AssertionError("expected save failure"); } catch(IllegalStateException expected) {}
        check("FAILED".equals(store.state.phase())&&ServerMaintenanceGate.paused(),"save failure never reopens");
        store.state=new State("DRAINING",10,5,"");store.failTerminal=true;
        try { service.tick(); } catch(java.sql.SQLException expected) {}
        check("MAINTENANCE".equals(store.state.phase()),"uncertain terminal commit cannot be overwritten FAILED");
        store.state=new State("MAINTENANCE",12,6,"");service.initialize();
        check(ServerMaintenanceGate.paused(),"restart preserves maintenance");
        service.heartbeatSafely();
        check(store.heartbeats==1,"independent heartbeat persists while coordinator is busy");
        MemoryStore diagnosticStore=new MemoryStore();
        diagnosticStore.state=new State("DRAINING",20,121,"");
        ServerMaintenanceService diagnosticService=new ServerMaintenanceService(diagnosticStore,()->{
            throw new IllegalStateException("diagnostic fixture cause");
        });
        diagnosticService.initialize();
        ByteArrayOutputStream diagnosticBytes=new ByteArrayOutputStream();
        PrintStream originalErr=System.err;
        try {
            System.setErr(new PrintStream(diagnosticBytes,true,StandardCharsets.UTF_8));
            Method tickSafely=ServerMaintenanceService.class.getDeclaredMethod("tickSafely");
            tickSafely.setAccessible(true);
            tickSafely.invoke(diagnosticService);
        } finally { System.setErr(originalErr); }
        String diagnosticLog=diagnosticBytes.toString(StandardCharsets.UTF_8);
        check(diagnosticLog.contains("diagnostic fixture cause")&&diagnosticLog.contains("IllegalStateException"),
                "maintenance log contains exception cause and stack trace");
        ServerMaintenanceGate.resume();
        System.out.println("PASS coordinator ordering, lost commits, save failure, replay, restart, reopen");
    }
}
