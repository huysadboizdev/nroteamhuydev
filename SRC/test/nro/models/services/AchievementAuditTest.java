package nro.models.services;

import java.io.*;
import java.util.*;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.item.Item;

/** Initial regression reproductions use the public achievement APIs and real packets. */
public final class AchievementAuditTest {
    static class Owner extends Player {
        byte[] packet;
        @Override public void sendMessage(Message m) { packet=m.getData(); }
    }
    static void check(boolean ok,String msg) { if(!ok)throw new AssertionError(msg); }
    static Owner owner() throws Exception {
        Owner p=new Owner();p.isPlayer=true;p.id=100123;p.nPoint.power=0;
        p.magicTree=PinkFixture.blank(nro.models.npc.MagicTree.class);
        for(int i=0;i<20;i++)p.achievement.add(new AchievementQuest(0,false));
        p.inventory.itemsBag.add(new Item()); return p;
    }
    static void fixture() throws Exception {
        Manager.ACHIEVEMENT_TEMPLATE.clear();
        for(int i=0;i<20;i++)Manager.ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate("Mission "+i,"Need 10",i==17?50000:20,10));
        PinkFixture.replace(Service.class,"instance",new Service(){
            @Override public void sendMoney(Player p){}
            @Override public void sendThongBao(Player p,String s){}
        });
        PinkFixture.replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
    }
    static void incomplete() throws Exception {
        Owner p=owner();AchievementService.gI().confirmAchievement(p,(byte)4);
        check(p.inventory.ruby==0&&p.inventory.gem==0&&!p.achievement.isRecieve(4),"incomplete achievement cannot grant or mark claimed");
    }
    static void duplicate() throws Exception {
        Owner p=owner();p.achievement.done(4,10);p.achievement.reward(4);
        AchievementService.gI().confirmAchievement(p,(byte)4);
        check(p.inventory.ruby==0&&p.inventory.gem==0,"already claimed achievement never pays again");
    }
    static void invalid() throws Exception {
        Owner p=owner();AchievementService.gI().confirmAchievement(p,(byte)-1);
        AchievementService.gI().confirmAchievement(p,(byte)127);
        check(p.inventory.ruby==0&&p.inventory.gem==0,"invalid IDs are harmless");
    }
    static void counters() throws Exception {
        Owner p=owner();p.achievement.done(4,Long.MAX_VALUE);p.achievement.done(4,1);
        check(p.achievement.getCompleted(4)>=10,"progress cannot overflow below completion");
        p.achievement.done(4,-Long.MAX_VALUE);check(p.achievement.getCompleted(4)>=10,"negative increments cannot undo progress");
    }
    static void wire() throws Exception {
        Owner p=owner();AchievementService.gI().openAchievementUI(p);
        var in=new DataInputStream(new ByteArrayInputStream(p.packet));
        check(in.readByte()==0&&in.readUnsignedByte()==20,"all 20 missions sent");
        for(int i=0;i<20;i++){in.readUTF();in.readUTF();check(in.readShort()==1000,"reward wire value must be 1000 ruby for mission "+i);in.readBoolean();in.readBoolean();}
        check(in.available()==0,"achievement packet stays compatible");
    }
    public static void main(String[] args) {
        try {fixture();switch(args[0]) {case "incomplete"->incomplete();case "duplicate"->duplicate();case "invalid"->invalid();case "counters"->counters();case "wire"->wire();default->throw new AssertionError(args[0]);}
            System.out.println("ACHIEVEMENT_AUDIT PASS "+args[0]);System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
