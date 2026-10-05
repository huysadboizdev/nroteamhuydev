package nro.models.shop_ky_gui;
import java.util.*;
import nro.models.intrinsic.*;
import nro.models.player.*;
import nro.models.services.*;
import nro.models.server.Manager;
import static nro.models.services.PinkFixture.*;
public final class IntrinsicMaxTest {
    static Intrinsic template(int id,int gender,int a,int b){Intrinsic i=new Intrinsic();i.id=id;i.gender=(byte)gender;i.name="fixture p0% đến p1 / p2% đến p3";i.paramFrom1=1;i.paramTo1=(short)a;i.paramTo2=(short)b;return i;}
    public static void main(String[] args)throws Exception{
        install();Manager.INTRINSICS.clear();
        for(int n=0;n<8;n++)for(Intrinsic i:List.of(template(0,3,0,0),template(1,0,25,0),template(3,0,35,35),template(8,1,25,0),template(16,2,25,0),template(23,3,300,0)))Manager.INTRINSICS.add(i);
        for(byte g=0;g<3;g++){
            final byte planet=g;
            List<Intrinsic> pool=IntrinsicService.gI().maxPool(g);check(pool.size()==(g==0?3:2),"deduplicated planet pool");
            check(pool.stream().noneMatch(i->i.id==0||i.gender!=planet&&i.gender!=3),"no empty or wrong planet");
            for(int index=0;index<pool.size();index++){final int pick=index;
                Intrinsic out=IntrinsicService.gI().rollMax(g,bound->pick);
                check(out.param1==out.paramTo1&&out.param2==out.paramTo2,"all stats at max");
                check(out!=pool.get(index)&&pool.get(index).param1==0,"never mutate template");
            }
        }
        replace(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        class Dao extends ConsignRegressionTest.MemoryDAO{
            @Override public int transactIntrinsic(String token,nro.models.database.RubyGiftDAO.Snapshot p,String intrinsic)throws java.sql.SQLException{return super.transact(token,"intrinsic_max",null,null,p);}
        }
        var dao=new Dao();var tx=new ConsignTransactions(dao,p->true);Player p=ConsignRegressionTest.owner(777);
        p.session.actived=false;p.playerIntrinsic=new IntrinsicPlayer();p.playerIntrinsic.intrinsic=template(1,0,25,0);p.playerIntrinsic.countOpen=4;p.effectSkill=new EffectSkill(p);
        Intrinsic old=p.playerIntrinsic.intrinsic;Intrinsic reward=IntrinsicService.gI().rollMax((byte)0,n->1);
        p.nPoint.power=9_999_999_999L;check(!tx.purchaseIntrinsicMax(p,reward).success()&&dao.writes==0,"10b boundary");p.nPoint.power=10_000_000_000L;
        p.inventory.ruby=99;check(!tx.purchaseIntrinsicMax(p,reward).success()&&dao.writes==0,"need100 ruby");p.inventory.ruby=100;
        dao.fail=true;check(!tx.purchaseIntrinsicMax(p,reward).success()&&p.inventory.ruby==100&&p.playerIntrinsic.intrinsic==old,"rollback preserves old intrinsic");dao.fail=false;
        check(tx.purchaseIntrinsicMax(p,reward).success()&&p.inventory.ruby==0&&p.playerIntrinsic.intrinsic==reward,"charge100 and publish");
        check(p.playerIntrinsic.countOpen==4&&p.inventory.gold==1000&&p.inventory.gem==100,"countOpen and other currencies unchanged");
        p.inventory.ruby=100;old=p.playerIntrinsic.intrinsic;dao.uncertain=true;
        check(!tx.purchaseIntrinsicMax(p,reward).success()&&ConsignTransactions.hasPending(p)&&p.inventory.ruby==100&&p.playerIntrinsic.intrinsic==old,"uncertain outcome held");
        IntrinsicService.gI().open(p);IntrinsicService.gI().openVip(p);IntrinsicService.gI().doinoitai(p);
        check(p.inventory.gold==1000&&p.inventory.gem==100&&p.playerIntrinsic.intrinsic==old,"ordinary/VIP cannot overwrite pending max");
        dao.outage=false;check(ConsignTransactions.reconcilePending(p)&&p.inventory.ruby==0,"reconcile one debit");check(ConsignTransactions.reconcilePending(p)&&p.inventory.ruby==0,"idempotent recovery");
        Player leaving=ConsignRegressionTest.owner(778);leaving.playerIntrinsic=new IntrinsicPlayer();leaving.playerIntrinsic.intrinsic=template(1,0,25,0);leaving.effectSkill=new EffectSkill(leaving);
        final boolean[] saved={false};var logoutTx=new ConsignTransactions(dao,x->true,x->{saved[0]=x.playerIntrinsic.intrinsic==reward&&x.inventory.ruby==0;return saved[0];});
        check(!logoutTx.purchaseIntrinsicMax(leaving,reward).success(),"logout unknown outcome");leaving.beforeDispose=true;dao.outage=false;
        check(ConsignTransactions.reconcilePending(leaving)&&saved[0]&&leaving.consignLogoutSaved,"logout saves published intrinsic and ruby before releasing gate");
        System.out.println("PASS IntrinsicMax: dedup planets/common, max both stats, template safety, fees, power, rollback, uncertain recovery");System.exit(0);
    }
}
