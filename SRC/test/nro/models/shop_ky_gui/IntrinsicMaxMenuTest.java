package nro.models.shop_ky_gui;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.lang.ref.WeakReference;
import nro.models.player.*;
import nro.models.services.*;
import nro.models.network.Message;
import nro.models.server.Manager;
import static nro.models.services.PinkFixture.*;
public final class IntrinsicMaxMenuTest {
    public static class Viewer extends Player {byte[] menu;@Override public void sendMessage(Message m){if(m.command==32)menu=m.getData();}}
    public static void main(String[] args)throws Exception {
        install();Manager.INTRINSICS.clear();Manager.INTRINSICS.add(IntrinsicMaxTest.template(1,0,25,0));Manager.INTRINSICS.add(IntrinsicMaxTest.template(23,3,300,0));
        replace(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        var dao=new ConsignRegressionTest.MemoryDAO(){@Override public int transactIntrinsic(String token,nro.models.database.RubyGiftDAO.Snapshot p,String data)throws java.sql.SQLException{return super.transact(token,"intrinsic_max",null,null,p);}};
        var tx=new ConsignTransactions(dao,p->true);
        var service=new IntrinsicService(){@Override protected ConsignTransactions transactions(){return tx;}};
        Player source=ConsignRegressionTest.owner(900);Viewer p=blank(Viewer.class);p.id=source.id;p.isPlayer=true;p.session=source.session;p.session.player=p;
        p.inventory=source.inventory;p.event=source.event;p.kolQuestState=source.kolQuestState;p.nPoint=source.nPoint;
        p.playerIntrinsic=new nro.models.intrinsic.IntrinsicPlayer();p.playerIntrinsic.intrinsic=IntrinsicMaxTest.template(1,0,25,0);p.effectSkill=new EffectSkill(p);
        service.showMenu(p);var reader=new DataInputStream(new ByteArrayInputStream(p.menu));reader.readShort();reader.readUTF();check(reader.readByte()==5,"five buttons");
        reader.readUTF();reader.readUTF();reader.readUTF();check(reader.readUTF().equals("Mở max")&&reader.readUTF().equals("Từ chối"),"new index3 preserves old selections");
        service.showConfirmOpenMax(p);check(p.idMark.getIndexMenu()==nro.models.consts.ConstNpc.CONFIRM_OPEN_INTRINSIC_MAX,"confirmation menu route");
        service.confirmMax(p,0);check(dao.writes==1&&p.inventory.ruby==0,"one confirmed roll");p.inventory.ruby=100;
        service.confirmMax(p,0);check(dao.writes==1&&p.inventory.ruby==100,"replay cannot debit again");
        service.showConfirmOpenMax(p);service.confirmMax(p,1);service.confirmMax(p,0);check(dao.writes==1,"cancel consumes choice");
        service.showConfirmOpenMax(p);p.gender=1;service.confirmMax(p,0);check(dao.writes==1,"changed planet denied");p.gender=0;
        service.showConfirmOpenMax(p);p.session=ConsignRegressionTest.owner(901).session;p.session.player=p;service.confirmMax(p,0);check(dao.writes==1,"changed session denied");
        service.showConfirmOpenMax(p);Field choices=IntrinsicService.class.getDeclaredField("maxChoices");choices.setAccessible(true);Map map=(Map)choices.get(service);Object choice=map.get(p);
        Constructor ctor=choice.getClass().getDeclaredConstructors()[0];ctor.setAccessible(true);map.put(p,ctor.newInstance(new WeakReference<>(p.session),(byte)0,System.currentTimeMillis()-120_001));
        service.confirmMax(p,0);check(dao.writes==1&&p.inventory.ruby==100,"expired confirmation no debit");
        service.showConfirmOpenMax(p);replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){throw new RuntimeException("lost notification");}@Override public void sendMoney(Player p){}@Override public void point(Player p){}});
        service.confirmMax(p,0);service.confirmMax(p,1);check(dao.writes==2&&p.inventory.ruby==0,"notification failure cannot repeat committed roll");
        System.out.println("PASS IntrinsicMaxMenu: five buttons, token replay/cancel/session/planet/expiry, notification failure");System.exit(0);
    }
}
