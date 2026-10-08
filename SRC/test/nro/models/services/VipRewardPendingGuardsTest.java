package nro.models.services;
import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import static nro.models.services.PinkFixture.*;
public class VipRewardPendingGuardsTest {
 public static void main(String[] args)throws Exception {
  install();replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
  Player p=player(8901);p.walletPurchasePending=new WalletPurchaseService.Pending("pending",50000,"vip_1",()->{});
  p.inventory.ruby=Integer.MAX_VALUE-199;Item capsule=item(722,1);p.inventory.itemsBag.add(capsule);
  PinkCapsuleService.open(p,capsule);
  check(capsule.quantity==1&&p.inventory.ruby==Integer.MAX_VALUE-199,"pending VIP reserves room before opening capsule");
  ItemMap bottle=blank(ItemMap.class);bottle.commesonMatchId="pending";
  check(!CommesonChallengeService.gI().beforePickup(p,bottle),"pending wallet forbids conflicting Commeson snapshot");
  for(int level=1;level<=4;level++){p.walletPurchasePending=new WalletPurchaseService.Pending("pending",1,"vip_"+level,()->{});check(VipRewardCatalog.reservedRuby(p)==new int[]{200,500,700,1000}[level-1],"reservation tier");}
  System.out.println("VipRewardPendingGuardsTest PASS ruby reservation and Commeson protection");
 }
}
