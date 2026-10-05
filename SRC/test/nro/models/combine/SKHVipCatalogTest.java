package nro.models.combine;
import java.util.*;
import java.lang.reflect.*;
import nro.models.item.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;
import nro.models.shop.*;
public class SKHVipCatalogTest {
static int[][] VIP={ {0,33,3,34,136,137,138,139,230,231,232,233},
{6,35,9,36,140,141,142,143,242,243,244,245},
{21,24,37,38,144,145,146,147,254,255,256,257},
{27,30,39,40,148,149,150,151,266,267,268,269},
{12,57,58,59,184,185,186,187,278,279,280,281},
{1,41,4,42,152,153,154,155,234,235,236,237},
{7,43,10,44,156,157,158,159,246,247,248,249},
{22,46,25,45,160,161,162,163,258,259,260,261},
{28,47,31,48,164,165,166,167,270,271,272,273},
{12,57,58,59,184,185,186,187,278,279,280,281},
{2,49,5,50,168,169,170,171,238,239,240,241},
{8,51,11,52,172,173,174,175,250,251,252,253},
{23,53,26,54,176,177,178,179,262,263,264,265},
{29,55,32,56,180,181,182,183,274,275,276,277},
{12,57,58,59,184,185,186,187,278,279,280,281}, };
static int[][] LEGACY={ {0,3,33,34,136,137,138,139,230,231,232,233},
{6,9,35,36,140,141,142,143,242,243,244,245},
{21,24,37,38,144,145,146,147,254,256,257},
{27,30,39,40,148,149,150,151,266,267,268,269},
{12,57,58,59,184,185,186,187,278,279,280,281},
{1,4,41,42,152,153,154,155,235,236,237},
{7,10,43,44,156,157,158,159,246,247,248,249},
{22,25,45,46,160,161,162,163,259,260,261},
{28,31,47,48,164,165,166,167,270,271,272,273},
{12,57,58,59,184,185,186,187,278,279,280,281},
{2,5,49,50,168,169,170,171,238,239,240,241},
{8,11,51,52,172,173,174,174,250,251,252,253},
{23,26,53,54,176,177,178,179,262,263,264,265},
{29,32,55,56,180,181,182,183,274,275,276,277},
{12,57,58,59,184,185,186,187,278,279,280,281}, };
static Class<?> catalog;
static int[][] BASE={{47},{6,27},{0},{7,28},{14}};
public static void main(String[] args) throws Exception {
try {catalog=Class.forName("nro.models.item.SKHCapsuleCatalog");} catch(ClassNotFoundException e){throw new AssertionError("VIP capsule catalog is missing",e);}
for(int p=0;p<3;p++)for(int t=0;t<5;t++){
check(Arrays.equals(ids("vipItems",p,t),VIP[p*5+t]),"actual level order");
check(Arrays.equals(ids("legacyItems",p,t),LEGACY[p*5+t]),"legacy changed");
int[] copy=ids("vipItems",p,t);copy[0]=-1;check(ids("vipItems",p,t)[0]>=0,"array shared");}
for(int p:new int[]{-1,3,99})reject(()->ids("vipItems",p,0));reject(()->ids("vipItems",0,5));
for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
for(int id=0;id<282;id++){Template.ItemTemplate x=new Template.ItemTemplate();x.id=(short)id;x.name="fixture";Manager.ITEM_TEMPLATES.add(x);}
for(int p=0;p<3;p++)for(int t=0;t<5;t++)for(int id:VIP[p*5+t]){Template.ItemTemplate x=Manager.ITEM_TEMPLATES.get(id);x.type=(byte)t;x.gender=(byte)(t==4?3:p);}
for(int sid:new int[]{1,2,3,31}){Shop s=new Shop();s.id=sid;TabShop tab=new TabShop();s.tabShops.add(tab);Manager.SHOPS.add(s);
for(int p=0;p<3;p++)for(int t=0;t<5;t++)for(int id:VIP[p*5+t]){ItemShop e=new ItemShop();e.temp=Manager.ITEM_TEMPLATES.get(id);
for(int opt:BASE[t])e.options.add(new Item.ItemOption(opt,sid==31?999999:id+sid+opt));e.options.add(new Item.ItemOption(50,999));tab.itemShops.add(e);}}
for(int p=0;p<3;p++)for(int t=0;t<5;t++)for(int id:VIP[p*5+t]){int set=sets(p)[4];Item out=build(id,set,p);
check(out.quantity==1&&out.template.id==id,"wrong item");for(int opt:BASE[t])check(value(out,opt)==id+3+opt,"wrong maximum");
check(out.itemOptions.size()==BASE[t].length+2+ActivationSet.effectIds(set).length,"unwanted options");check(value(out,set)==1&&value(out,30)==1,"set marker");
for(int effect:ActivationSet.effectIds(set))check(value(out,effect)==1,"set effects");out.itemOptions.get(0).param=-1;check(value(build(id,set,p),BASE[t][0])==id+3+BASE[t][0],"options shared");}
check(Arrays.equals(sets(0),new int[]{128,129,127,233,245}),"earth sets");check(Arrays.equals(sets(1),new int[]{130,131,132,233,237}),"namek sets");check(Arrays.equals(sets(2),new int[]{133,135,134,233,241}),"xayda sets");
for(int p:new int[]{-1,3}){reject(()->sets(p));reject(()->build(0,128,p));}reject(()->build(0,130,0));reject(()->build(1,128,0));reject(()->build(999,128,0));
Template.ItemTemplate x=Manager.ITEM_TEMPLATES.get(0);x.gender=1;reject(()->build(0,128,0));x.gender=0;x.type=2;reject(()->build(0,128,0));x.type=0;
// Real SQL level-one pants/shoes have only their HP/KI option, without regeneration.
for (Shop shop : Manager.SHOPS) if (shop.id <= 3) {
    for (TabShop villageTab : shop.tabShops) for (ItemShop entry : villageTab.itemShops) {
        int id = entry.temp.id;
        if ((id >= 6 && id <= 8) || (id >= 27 && id <= 29)) {
            entry.options.removeIf(option -> option.optionTemplate.id == 27 || option.optionTemplate.id == 28);
        }
    }
}
for (int planet = 0; planet < 3; planet++) for (int type : new int[]{1, 3}) {
    int id = VIP[planet * 5 + type][0];
    Item lowest = build(id, sets(planet)[0], planet);
    check(value(lowest, BASE[type][0]) == id + 3 + BASE[type][0], "level-one primary maximum");
    check(lowest.itemOptions.size() == 4, "level-one output invented regeneration");
}
// Independent base maxima can come from different village entries/tabs.
for(Shop shop:Manager.SHOPS) if(shop.id<=3) for(TabShop villageTab:shop.tabShops) for(ItemShop villageEntry:villageTab.itemShops) if(villageEntry.temp.id==245) {
    villageEntry.options.clear();
    if(shop.id==1) villageEntry.options.add(new Item.ItemOption(6,48000));
    if(shop.id==2) villageEntry.options.add(new Item.ItemOption(27,1600));
    if(shop.id==3) { villageEntry.options.add(new Item.ItemOption(6,47000)); villageEntry.options.add(new Item.ItemOption(27,1500)); }
}
Item split=build(245,128,0);check(value(split,6)==48000&&value(split,27)==1600,"independent base maxima across shops");
for(int p=0;p<3;p++) for(int set:sets(p)) {
    Item output=build(VIP[p*5][0],set,p);
    check(value(output,set)==1,"set primary");for(int effect:ActivationSet.effectIds(set))check(value(output,effect)==1,"all set effects");
}
int[] copiedSets=sets(0);copiedSets[0]=-1;check(sets(0)[0]==128,"sets array shared");
Template.ItemOptionTemplate marker=Manager.ITEM_OPTION_TEMPLATES.get(30);Manager.ITEM_OPTION_TEMPLATES.set(30,null);reject(()->build(0,128,0));Manager.ITEM_OPTION_TEMPLATES.set(30,marker);
Template.ItemTemplate saved=Manager.ITEM_TEMPLATES.get(0);Manager.ITEM_TEMPLATES.set(0,null);reject(()->build(0,128,0));Manager.ITEM_TEMPLATES.set(0,saved);
Manager.SHOPS.clear();reject(()->build(0,128,0));Shop s=new Shop();s.id=1;TabShop tab=new TabShop();s.tabShops.add(tab);Manager.SHOPS.add(s);ItemShop e=new ItemShop();e.temp=Manager.ITEM_TEMPLATES.get(245);e.options.add(new Item.ItemOption(6,999));tab.itemShops.add(e);reject(()->build(245,128,0));
System.out.println("PASS: all 15 catalogs, 180 maximum-stat outputs, legacy anomalies, cloning and invalid/missing data");}
static int[] ids(String name,int p,int t)throws Exception{return(int[])catalog.getMethod(name,int.class,int.class).invoke(null,p,t);}
static int[] sets(int p)throws Exception{return(int[])catalog.getMethod("sets",int.class).invoke(null,p);}
static Item build(int id,int set,int p)throws Exception{return(Item)ItemService.class.getMethod("createItemSKHVipMax",int.class,int.class,int.class).invoke(ItemService.gI(),id,set,p);}
static int value(Item item,int id){for(Item.ItemOption o:item.itemOptions)if(o.optionTemplate.id==id)return o.param;throw new AssertionError("missing option "+id);}
static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
interface Action{void run()throws Exception;}
static void reject(Action a)throws Exception{try{a.run();}catch(InvocationTargetException e){if(e.getCause() instanceof IllegalArgumentException||e.getCause() instanceof IllegalStateException)return;throw e;}throw new AssertionError("invalid accepted");}
}