package nro.models.player;
import java.util.*;
import nro.models.item.Item;
import nro.models.services.InventoryService;
public final class InventoryCapacityTest {
 public static void main(String[] args) throws Exception {
  if(Inventory.MAX_ITEMS_BAG!=200 || Inventory.MAX_ITEMS_BOX!=200) throw new AssertionError("bag/box capacity must both be 200");
  Player p=new Player(); for(int i=0;i<200;i++)p.inventory.itemsBag.add(new Item());
  if(InventoryService.gI().getCountEmptyBag(p)!=200)throw new AssertionError("200 empty slots must not wrap");
  Item last=p.inventory.itemsBag.get(199); if(InventoryService.gI().getIndexBag(p,last)!=199)throw new AssertionError("slot 199");
  var pad=Inventory.class.getMethod("padSlots",List.class,int.class); List<Item> list=new ArrayList<>(); Item first=new Item();list.add(first);
  pad.invoke(null,list,200);if(list.size()!=200||list.get(0)!=first||list.get(1)==list.get(2))throw new AssertionError("padding preserves slots with fresh empties");
  list.add(new Item());try{pad.invoke(null,list,200);throw new AssertionError("oversize accepted");}catch(java.lang.reflect.InvocationTargetException e){if(!(e.getCause() instanceof IllegalArgumentException))throw e;}
  if(list.size()!=201)throw new AssertionError("oversize truncated");
  p.combineNew.typeCombine=-999; p.combineNew.itemsCombine.add(first);
  nro.models.combine.CombineService.gI().showInfoCombine(p,new int[]{199,199});
  if(p.combineNew.itemsCombine.size()!=1||p.combineNew.itemsCombine.get(0)!=first)throw new AssertionError("duplicate combine must not replace prior selection");
  System.out.println("PASS capacity/count/high slot/padding/nontruncation");
 }
}
