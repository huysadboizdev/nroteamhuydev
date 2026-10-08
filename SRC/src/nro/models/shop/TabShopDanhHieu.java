package nro.models.shop;
import java.util.*;
import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.player_badges.*;
import nro.models.task.BadgesTaskService;
public class TabShopDanhHieu extends TabShop {
 public TabShopDanhHieu(TabShop base,Player player){
  synchronized(player){
   this.itemShops=new ArrayList<>();this.shop=base.shop;this.id=base.id;this.name=base.name;
   Set<Integer> owned=new HashSet<>(BagesTemplate.listEffect(player));
   int selectedGoDauTre=GoDauTreShopRows.selectedItem(player,base.itemShops);
   for(ItemShop original:base.itemShops){
    if(GoDauTreShopRows.isFamily(original.temp.id)&&original.temp.id!=selectedGoDauTre)continue;
    if(original.temp.gender!=player.gender && original.temp.gender<=2)continue;
    if(owned.contains((int)original.temp.id)!=false)continue;
    BagesTemplate title=BagesTemplate.fineBadgesbyIdItem(original.temp.id);if(title==null)continue;
    ItemShop copy=new ItemShop(original);copy.options.clear();
    for(Item.ItemOption opt:title.options)if(opt.optionTemplate.id!=220)copy.options.add(new Item.ItemOption(opt.optionTemplate.id,opt.param));
    copy.options.add(new Item.ItemOption(220,BadgesTaskService.sendPercenBadgesTask(player,title.idEffect)));
    this.itemShops.add(copy);
   }
  }
 }
}
