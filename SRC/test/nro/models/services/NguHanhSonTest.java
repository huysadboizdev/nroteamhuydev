package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.map.Zone;
import nro.models.server.Manager;

/** Offline checks of actual region exp and atomic peach exchange. */
public final class NguHanhSonTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static Player player(int map)throws Exception {
        Player p=new Player();p.isPlayer=true;p.id=987654321;p.nPoint.hp=p.nPoint.hpMax=100;
        var m=PinkFixture.blank(nro.models.map.Map.class);m.mapId=map;
        p.zone=new Zone(m,0,20);return p;
    }
    public static void main(String[] args)throws Exception {
        PinkFixture.install();Manager.RATE_EXP_SERVER=1;
        routes();
        PinkFixture.replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){
                var t=PinkFixture.template(id);
                if(id>=544&&id<=548||id==1698){t.type=5;t.isUpToUp=false;}return t;
            }
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){
                var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="option #";return t;
            }
        });
        check(nro.models.data.DataGame.vsMap>=7,"Updated map/NPC catalog must invalidate client cache");
        var speedMethod=nro.models.player.NPoint.class.getDeclaredMethod("setSpeed");speedMethod.setAccessible(true);
        Player runner=player(124);
        for(int percent:new int[]{0,12,13,25,50,75,99,100}){
            runner.nPoint.tlSpeed=percent;speedMethod.invoke(runner.nPoint);
            check(runner.nPoint.speed==8+8*percent/100,"Fractional costume speed bonus "+percent);
        }
        peachDrops();
        for(int id:new int[]{122,123,124,0,121,125}){
            Player p=player(id);long expected=id>=122&&id<=124?5000:1000;
            check(p.nPoint.calSucManhTiemNang(1000)==expected,"TNSM multiplier map "+id);
            if(id==124)check(p.nPoint.calSucManhTiemNang(Long.MAX_VALUE)==Long.MAX_VALUE,"Exp saturation");
        }
        Class<?> cls=Class.forName("nro.models.services.NguHanhSonService");
        Method exchange=cls.getMethod("exchange",Player.class,int.class);
        PinkFixture.replace(InventoryService.class,"I",new InventoryService(){
            @Override public void sendItemBags(Player p){}
            @Override public boolean addItemBag(Player p,Item item){return addItemList(p.inventory.itemsBag,item);}
        });
        for(int id:new int[]{544,545,546,547,548,1698}){
            Player p=player(124);int currency=id<=546?541:542;int price=id<=546?200:id==1698?300:150;
            Item first=PinkFixture.item(currency,price/2),second=PinkFixture.item(currency,price-price/2+7);
            p.inventory.itemsBag=new ArrayList<>(List.of(first,second,new Item()));
            check((boolean)exchange.invoke(null,p,id),"Successful costume "+id);
            check(second.quantity==7,"Exact split-stack payment "+id);
            Item reward=p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).findFirst().orElseThrow();
            check(reward.getOptionById(93)!=null,"Every costume has expiration");
            Set<Integer> attacks=new HashSet<>(),hp=new HashSet<>(),days=new HashSet<>();
            for(int sample=0;sample<100;sample++){
                for(int option:new int[]{50,94,103,77,101,100,114}){
                    var stat=reward.getOptionById(option);
                    int cap=option==50?30:option==94?50:100;
                    check(stat!=null&&stat.param>=1&&stat.param<=cap,"Random stat range "+id+"/"+option);
                }
                check(reward.getOptionById(106)!=null,"Cold immunity retained");
                check(reward.getOptionById(93).param>=3&&reward.getOptionById(93).param<=7,"Three-to-seven-day costume");
                check(reward.getOptionById(174).param==2026,"Event year");
                days.add(reward.getOptionById(93).param);
                attacks.add(reward.getOptionById(50).param);hp.add(reward.getOptionById(77).param);
                if(sample<99){
                    Player roll=player(124);
                    roll.inventory.itemsBag=new ArrayList<>(List.of(PinkFixture.item(currency,price),new Item()));
                    check((boolean)exchange.invoke(null,roll,id),"Fresh random exchange");
                    reward=roll.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).findFirst().orElseThrow();
                }
            }
            check(attacks.size()>1&&hp.size()>1&&days.size()>1,"Stats and duration vary between exchanges "+id);
            check(!(boolean)exchange.invoke(null,p,id)&&second.quantity==7,"Insufficient payment rejected");
        }
        Player p=player(124);Item peach=PinkFixture.item(541,200);
        p.inventory.itemsBag=new ArrayList<>(List.of(peach));
        check(!(boolean)exchange.invoke(null,p,544)&&peach.quantity==200,"Full bag does not consume");
        p.inventory.itemsBag.add(new Item());
        LiveSyncCommandGate.mark(p.id,0);
        check(!(boolean)exchange.invoke(null,p,544)&&peach.quantity==200,"Pending data sync blocks payment");
        LiveSyncCommandGate.clear(p.id,0);
        check(!(boolean)exchange.invoke(null,p,1698)&&peach.quantity==200,"Wrong currency rejected");
        check(!(boolean)exchange.invoke(null,p,1593)&&peach.quantity==200,"Unlisted item rejected");
        p.zone.map.mapId=0;
        check(!(boolean)exchange.invoke(null,p,544)&&peach.quantity==200,"Exchanges only at final map");
        p.zone.map.mapId=124;
        Player concurrent=player(124);
        concurrent.inventory.itemsBag=new ArrayList<>(List.of(PinkFixture.item(541,200),new Item(),new Item()));
        var success=new java.util.concurrent.atomic.AtomicInteger();
        Runnable swap=()->{try{if((boolean)exchange.invoke(null,concurrent,544))success.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}};
        Thread a=new Thread(swap),b=new Thread(swap);a.start();b.start();a.join();b.join();
        check(success.get()==1,"Concurrent requests cannot overspend one payment");
        check(concurrent.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==544).count()==1,"Only one concurrent reward");
        Player menuOwner=player(124);menuOwner.location.x=1080;menuOwner.location.y=264;
        menuOwner.inventory.itemsBag=new ArrayList<>(List.of(PinkFixture.item(541,400),new Item(),new Item()));
        var guide=new nro.models.npc_list.TayDuGuide(124,1,1080,264,48,4468);
        guide.openBaseMenu(menuOwner);guide.confirmMenu(menuOwner,0);guide.confirmMenu(menuOwner,0);
        check(menuOwner.idMark.getIndexMenu()==125544,"Selected offer opens confirmation");
        guide.confirmMenu(menuOwner,0);guide.confirmMenu(menuOwner,0);
        check(menuOwner.inventory.itemsBag.get(0).quantity==200,"Replayed confirmation cannot charge again");
        check(menuOwner.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==544).count()==1,"One reward from confirmed menu");
        menuOwner.idMark.setIndexMenu(125548);guide.confirmMenu(menuOwner,0);
        check(menuOwner.inventory.itemsBag.get(0).quantity==200,"Other guide's offers cannot be forged");
        menuOwner.idMark.setIndexMenu(125544);menuOwner.location.x=0;guide.confirmMenu(menuOwner,0);
        check(menuOwner.inventory.itemsBag.get(0).quantity==200,"Remote menu cannot spend peaches");
        PinkFixture.replace(InventoryService.class,"I",new InventoryService(){
            @Override public boolean addItemBag(Player owner,Item item){return false;}
            @Override public void sendItemBags(Player owner){}
        });
        check(!(boolean)exchange.invoke(null,p,544)&&peach.quantity==200,"Failed grant does not consume");
        System.out.println("NGU_HANH_SON PASS exp x5/saturation and exchanges/payment/full bag/failure/map guards");
    }
    static void peachDrops()throws Exception {
        var field=nro.models.utils.Util.class.getDeclaredField("rand");field.setAccessible(true);
        ((Random)field.get(null)).setSeed(20261010L);
        for(int map:new int[]{122,123,124,121,125})for(boolean petKill:new boolean[]{false,true}){
            Player master=player(map);master.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();
            Player killer=petKill?new nro.models.player.Pet(master):master;
            if(petKill)killer.id=-master.id;
            killer.zone=master.zone;killer.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();
            var mob=new nro.models.mob.Mob();mob.tempId=50;mob.zone=master.zone;
            int drops=0,ripe=0;
            for(int sample=0;sample<5000;sample++){
                for(var drop:mob.getItemMobReward(killer,60,288))if(drop.itemTemplate.id==541||drop.itemTemplate.id==542){
                    drops++;if(drop.itemTemplate.id==542)ripe++;
                    check(drop.quantity==1&&drop.playerId==master.id,"Peach amount and owner for pet/master");
                }
                master.zone.items.clear();
            }
            if(map>=122&&map<=124){
                check(drops>=850&&drops<=1150,"20% base peach drop map "+map+" pet="+petKill+" got="+drops);
                check(ripe>0&&ripe<drops,"Both peach types drop");
            }else check(drops==0,"No peaches outside region");
        }
        System.out.println("PEACH_DROP PASS 20percent/3maps/master+pet/owner/outside");
    }
    static void routes()throws Exception {
        var fixtures=(org.json.simple.JSONArray)org.json.simple.JSONValue.parse(java.nio.file.Files.readString(
            java.nio.file.Path.of("test/fixtures/ngu-hanh-routes.json")));
        Manager.MAPS.clear();
        for(Object raw:fixtures){
            var row=(org.json.simple.JSONObject)raw;int id=((Number)row.get("id")).intValue();
            var m=PinkFixture.blank(nro.models.map.Map.class);m.mapId=id;m.type=0;
            m.tileMap=nro.models.data.TileMapData.read(id);m.mapWidth=m.tileMap[0].length*24;m.mapHeight=m.tileMap.length*24;
            m.tileTop=nro.models.map.service.MapService.gI().readTileIndexTileType(2)[0];
            m.wayPoints=new ArrayList<>();m.zones=new ArrayList<>();m.zones.add(new Zone(m,0,15));
            for(Object entry:(org.json.simple.JSONArray)row.get("waypoints")){
                var v=(org.json.simple.JSONArray)entry;var wp=new nro.models.map.WayPoint();
                wp.minX=((Number)v.get(1)).shortValue();wp.minY=((Number)v.get(2)).shortValue();
                wp.maxX=((Number)v.get(3)).shortValue();wp.maxY=((Number)v.get(4)).shortValue();
                wp.goMap=((Number)v.get(7)).intValue();wp.goX=((Number)v.get(8)).shortValue();wp.goY=((Number)v.get(9)).shortValue();
                m.wayPoints.add(wp);
            }
            Manager.MAPS.add(m);
        }
        var maps=nro.models.map.service.MapService.gI();
        for(int[] probe:new int[][]{{122,1380,432,123},{123,36,384,122},{123,1476,360,124},{124,36,360,123}}){
            Player p=player(probe[0]);p.zone=maps.getMapById(probe[0]).zones.get(0);
            p.location.x=probe[1];p.location.y=probe[2];
            var wp=maps.getWaypointPlayerIn(p);
            check(wp!=null&&wp.goMap==probe[3],"Actual waypoint lookup at walking height "+Arrays.toString(probe));
            var zone=maps.getMapCanJoin(p,wp.goMap,-1);
            check(zone!=null&&zone.map.mapId==probe[3],"Normal player can join next map");
            check(zone.map.yPhysicInTop(wp.goX,0)==wp.goY,"Actual arrival terrain "+wp.goMap);
        }
        check(maps.getMapById(122).yPhysicInTop(60,0)==288,"Village arrival on left of first map");
        System.out.println("NGU_HANH_ROUTE PASS actual waypoint lookup/join/terrain in both directions");
    }
}
