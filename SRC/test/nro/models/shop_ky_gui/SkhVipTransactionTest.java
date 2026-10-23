package nro.models.shop_ky_gui;

import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.combine.*;
import nro.models.database.RubyGiftDAO;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

/** Offline receipt tests: never constructs a live database connection. */
public final class SkhVipTransactionTest {
    static final class RecordingDAO extends ConsignRegressionTest.MemoryDAO {
        RubyGiftDAO.Snapshot snapshot;
        @Override public int transact(String token,String action,ConsignItem before,ConsignItem after,
                RubyGiftDAO.Snapshot staged)throws java.sql.SQLException {
            snapshot=staged;
            return super.transact(token,action,before,after,staged);
        }
    }
    private static ConsignTransactions.Result upgrade(ConsignTransactions tx,Player p)throws Exception {
        return (ConsignTransactions.Result)ConsignTransactions.class
                .getMethod("upgradeSkhVip",Player.class,IntUnaryOperator.class).invoke(tx,p,(IntUnaryOperator)b->b-1);
    }
    private static Player playerWithPreview()throws Exception {
        Player p=ConsignRegressionTest.owner(901);
        p.combineNew=new Combine(); p.idMark=new IDMark();
        p.inventory.gold=5_000_000_000L;
        Item main=item(650,1),fodder=item(555,1);
        main.template.type=0;main.template.gender=0;fodder.template.type=0;fodder.template.gender=0;
        p.inventory.itemsBag.set(0,main);p.inventory.itemsBag.set(1,fodder);
        p.combineNew.setTypeCombine(CombineService.NANG_SKH_VIP);
        p.combineNew.skhVipSelection=NangSKHVip.select(p,new int[]{0,1});
        p.combineNew.itemsCombine.add(main);p.combineNew.itemsCombine.add(fodder);
        return p;
    }
    public static void main(String[] args)throws Exception {
        try {ConsignTransactions.class.getMethod("upgradeSkhVip",Player.class,IntUnaryOperator.class);}
        catch(NoSuchMethodException missing){throw new AssertionError("VIP upgrade must use shared receipt transaction",missing);}
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){return template(id);}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){
                var option=new nro.models.player_system.Template.ItemOptionTemplate();option.id=id;return option;
            }
            @Override public Item createActivatedDivineItem(int templateId,int setId,int planet){
                Item reward=item(templateId,1);reward.itemOptions.add(new Item.ItemOption(setId,1));return reward;
            }
        });
        ConsignRegressionTest.set(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        RecordingDAO dao=new RecordingDAO();ConsignTransactions tx=new ConsignTransactions(dao,p->true);
        Player p=playerWithPreview();List<Item> original=p.inventory.itemsBag;
        if(args[0].startsWith("claim")) {
            p.skhVipRewardPlanets=new ArrayList<>(List.of(1));
            if(args[0].equals("claim-rollback"))dao.fail=true;
            if(args[0].equals("claim-uncertain"))dao.uncertain=true;
            if(args[0].equals("claim-full")||args[0].equals("claim-legacy-full")) {
                for(int n=0;n<p.inventory.itemsBag.size();n++)p.inventory.itemsBag.set(n,item(650,1));
            }
            if(args[0].equals("claim-legacy-full")) {
                p.skhVipRewardPlanets.clear();p.skhVipDivineProgress=50;p.skhVipDestroyerProgress=50;p.skhVipPaidPairs=50;
            }
            var claim=tx.claimSkhVip(p);
            if(args[0].equals("claim-full"))check(!claim.success()&&dao.writes==0&&p.skhVipRewardPlanets.equals(List.of(1)),"full bag preserves queued right without write");
            else if(args[0].equals("claim-legacy-full")) {
                check(claim.success()&&dao.writes==1&&p.skhVipDivineProgress==0&&p.skhVipRewardPlanets.equals(List.of(0)),"legacy right gets fixed owner planet and persists despite full bag");
                check(!tx.claimSkhVip(p).success()&&dao.writes==1,"legacy conversion is idempotent");
            } else if(args[0].equals("claim-rollback")) {
                check(!claim.success()&&p.inventory.itemsBag==original&&p.skhVipRewardPlanets.equals(List.of(1)),"failed claim preserves bag and queued right");
            } else {
                if(args[0].equals("claim-uncertain")) {
                    check(!claim.success()&&p.inventory.itemsBag==original&&p.skhVipRewardPlanets.equals(List.of(1)),"uncertain claim cannot erase right early");
                    check(!tx.claimSkhVip(p).success()&&dao.writes==1,"pending claim blocks duplicate grant");
                    dao.outage=false;check(ConsignTransactions.reconcilePending(p),"claim receipt recovers");
                } else check(claim.success(),"queued reward commits");
                check(p.skhVipRewardPlanets.isEmpty()&&p.inventory.itemsBag.get(2).isSKH(),"committed claim exchanges exactly one queued right for reward");
                check(p.inventory.gold==5_000_000_000L&&p.inventory.ruby==100,"claim charges no currency");
                check(!tx.claimSkhVip(p).success()&&dao.writes==1,"received reward cannot replay");
            }
            System.out.println("PASS SkhVipTransaction "+args[0]);System.exit(0);
        }
        if(args[0].equals("rollback"))dao.fail=true;
        if(args[0].equals("uncertain")||args[0].equals("absent"))dao.uncertain=true;
        var result=upgrade(tx,p);
        if(args[0].equals("rollback")) {
            check(!result.success()&&p.inventory.itemsBag==original&&p.inventory.gold==5_000_000_000L,"rollback preserves bag and gold");
            check(p.skhVipDivineProgress==0&&p.skhVipDestroyerProgress==0,"rollback preserves progress");
            check(!ConsignTransactions.hasPending(p)&&p.combineNew.skhVipSelection==null,"rollback releases gate and requires new preview");
        } else if(args[0].equals("uncertain")||args[0].equals("absent")) {
            check(!result.success()&&ConsignTransactions.hasPending(p),"uncertain receipt reserves player");
            check(p.inventory.itemsBag==original&&p.inventory.gold==5_000_000_000L&&p.skhVipDivineProgress==0,"no early publish");
            check(!upgrade(tx,p).success()&&dao.writes==1,"pending blocks replay");
            check(!ConsignTransactions.reconcilePending(p),"outage retains pending");
            dao.outage=false;if(args[0].equals("absent"))dao.receipts.clear();
            check(ConsignTransactions.reconcilePending(p),"resolved receipt releases gate");
            boolean committed=args[0].equals("uncertain");
            check(p.inventory.gold==(committed?4_000_000_000L:5_000_000_000L)&&p.skhVipDivineProgress==(committed?1:0),"receipt alone determines publish");
            check(ConsignTransactions.reconcilePending(p)&&dao.writes==1,"recovery does not replay");
        } else {
            check(result.success()&&dao.writes==1,"successful stage persists once");
            check(p.inventory.gold==4_000_000_000L&&!p.inventory.itemsBag.get(1).isNotNullItem(),"fee and fodder publish");
            check(p.skhVipDivineProgress==1&&p.skhVipDestroyerProgress==1,"failed upgrade still counts paid pair");
            String vip=(String)RubyGiftDAO.Snapshot.class.getMethod("vip").invoke(dao.snapshot);
            check(vip!=null&&dao.snapshot.inventory().startsWith("[4000000000,"),"inventory gold and VIP staged together");
            check(!upgrade(tx,p).success()&&dao.writes==1,"consumed preview cannot replay");
        }
        System.out.println("PASS SkhVipTransaction "+args[0]);System.exit(0);
    }
}

