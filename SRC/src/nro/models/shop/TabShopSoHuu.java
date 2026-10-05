package nro.models.shop;
import java.util.*;
import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.player_badges.*;
import nro.models.task.BadgesTaskService;
public class TabShopSoHuu extends TabShop {
 public TabShopSoHuu(TabShop base,Player player){
  synchronized(player){
   this.itemShops=new ArrayList<>();this.shop=base.shop;this.id=base.id;this.name=base.name;
   Set<Integer> owned=new HashSet<>(BagesTemplate.listEffect(player));
   for(ItemShop original:base.itemShops){
    if(original.temp.gender!=player.gender && original.temp.gender<=2)continue;
    if(owned.contains((int)original.temp.id)!=true)continue;
    BagesTemplate title=BagesTemplate.fineBadgesbyIdItem(original.temp.id);if(title==null)continue;
    ItemShop copy=new ItemShop(original);copy.options.clear();
    for(Item.ItemOption opt:title.options)if(opt.optionTemplate.id!=220)copy.options.add(new Item.ItemOption(opt.optionTemplate.id,opt.param));
    for(Item.ItemOption opt:copy.options)if(opt.optionTemplate.id==93)opt.param=BadgesTaskService.sendDay(player,title.idEffect);
    this.itemShops.add(copy);
   }
  }
 }
}
