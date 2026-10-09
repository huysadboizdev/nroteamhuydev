package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public final class HighDamageNoticeTest {
    public static void main(String[] args) throws Exception {
        Class<?> type;
        try { type=Class.forName("nro.models.services.HighDamageNoticeService"); }
        catch(ClassNotFoundException missing) { throw new AssertionError("Missing high damage announcement feature"); }
        AtomicLong clock=new AtomicLong(10_000);
        List<String> world=Collections.synchronizedList(new ArrayList<>()), info=Collections.synchronizedList(new ArrayList<>());
        Object service=type.getConstructor(LongSupplier.class,Consumer.class,Consumer.class).newInstance((LongSupplier)clock::get,(Consumer<String>)world::add,(Consumer<String>)info::add);
        Method report=type.getDeclaredMethod("report",Player.class,String.class,long.class); report.setAccessible(true);
        Player p=player(1); p.name="abc"; p.inventory.gem=77; p.idMark=new nro.models.player.IDMark(); p.idMark.setLastTimeChatGlobal(1234);
        report.invoke(service,p,"Kamejoko",19_999_999L); report.invoke(service,p,"Kamejoko",20_000_000L);
        check(world.isEmpty(),"strict threshold");
        report.invoke(service,p,"Kamejoko",25_000_000L);
        check(world.equals(List.of("Người chơi abc dùng chiêu Kamejoko với mức sát thương là 25.000.000.")),"exact text");
        check(world.equals(info),"identical dual channels");
        check(p.inventory.gem==77&&p.idMark.getLastTimeChatGlobal()==1234,"no currency/chat mutation");
        clock.set(11_000); report.invoke(service,p,"Kamejoko",25_000_000L); check(world.size()==1,"player cooldown");
        Player second=player(2);second.name="second";
        report.invoke(service,second,"Kamejoko",20_000_001L);check(world.size()==2,"rejected player hit must not consume global cooldown");
        clock.set(11_999); Player third=player(3);third.name="third";report.invoke(service,third,"Kamejoko",25_000_000L);check(world.size()==2,"global cooldown");
        clock.set(12_000);report.invoke(service,third,"Kamejoko",25_000_000L);check(world.size()==3,"global boundary / rejected hit must not consume player cooldown");
        clock.set(15_000);report.invoke(service,p,"Kamejoko",25_000_000L);check(world.size()==4,"player exact boundary");
        clock.set(20_000);p.name="|5|abc\n\u0000"+"😀".repeat(100);report.invoke(service,p,"|3|Skill\t"+"long".repeat(100),2_147_483_647L);
        String text=world.get(4);check(text.length()<=100&&!text.contains("|")&&!text.contains("\n")&&!text.contains("\u0000")&&text.endsWith("2.147.483.647."),"safe labels and intact damage");
        check(world.equals(info),"same truncated text");
        clock.set(30_000);
        for(String flag:List.of("isBot","isBoss","isPet","isNewPet","isNewPet1")) { Player excluded=player(100);Player.class.getField(flag).setBoolean(excluded,true);report.invoke(service,excluded,"Kamejoko",25_000_000L); }
        report.invoke(service,null,"Kamejoko",25_000_000L);report.invoke(service,p,null,25_000_000L);check(world.size()==5,"excluded attackers/missing skill");
        ExecutorService pool=Executors.newFixedThreadPool(8);List<Future<?>> hits=new ArrayList<>();
        for(int i=0;i<64;i++) {Player racing=player(200+i);racing.name="racer";hits.add(pool.submit(()->{try{report.invoke(service,racing,"Kamejoko",25_000_000L);}catch(Exception e){throw new RuntimeException(e);}}));}
        for(Future<?> hit:hits)hit.get();pool.shutdown();check(world.size()==6&&world.equals(info),"atomic concurrent admission");
        Object failed=type.getConstructor(LongSupplier.class,Consumer.class,Consumer.class).newInstance((LongSupplier)clock::get,(Consumer<String>)s->{throw new IllegalStateException("sender failed");},(Consumer<String>)info::add);
        report.invoke(failed,second,"Kamejoko",25_000_000L);check(info.size()==7,"world failure must still attempt info");
        List<Integer> packets=new ArrayList<>();List<String> payloads=new ArrayList<>();
        replace(Service.class,"instance",new Service(){
            @Override public void sendMessAllPlayer(nro.models.network.Message msg) {
                try {
                    packets.add((int)msg.command);
                    var in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(msg.getData()));
                    if(msg.command==92){check(in.readUTF().equals("Hệ thống"),"world system sender");payloads.add(in.readUTF().substring(3));}
                    else if(msg.command==-25)payloads.add(in.readUTF());
                }catch(java.io.IOException failure){throw new RuntimeException(failure);}
            }
        });
        Object productionSinks=type.getConstructor(LongSupplier.class,Consumer.class,Consumer.class).newInstance((LongSupplier)clock::get,(Consumer<String>)ChatGlobalService::sendSystemNotice,(Consumer<String>)s->Service.gI().sendThongBaoAllPlayer(s));
        report.invoke(productionSinks,second,"Kamejoko",25_000_000L);
        check(packets.equals(List.of(92,-25))&&payloads.size()==2&&payloads.get(0).equals(payloads.get(1)),"real packet 92/-25 identical text exactly once");
        for(int i=0;i<1000;i++){clock.addAndGet(1000);Player churn=player(10000+i);churn.name="churn";report.invoke(service,churn,"Kamejoko",25_000_000L);}
        Field players=type.getDeclaredField("players");players.setAccessible(true);check(((Map<?,?>)players.get(service)).size()<=5,"bounded cooldown retention under attacker churn");
        int emitted=world.size();clock.addAndGet(100_000);report.invoke(service,p,"Kamejoko",20_000_000L);check(world.size()==emitted,"cooldown drops never form backlog");
        System.out.println("PASS HighDamageNoticeTest threshold, cooldowns, dual sinks, labels, exclusions, concurrency, wallet/chat isolation, sender failure");
    }
}
