package nro.models.services;
import java.util.*;
import java.lang.reflect.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
public class SparseItemTemplateTest {
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  var list=Manager.ITEM_TEMPLATES;list.clear();
  for(int id:new int[]{0,1,2000,2019,2020,2026,2027,2030,2039}){var t=new Template.ItemTemplate();t.id=(short)id;t.name="actual "+id;t.description="desc";t.type=5;t.iconID=(short)id;list.add(t);}
  Class<?> c=Class.forName("nro.models.item.ItemTemplateIndex");c.getMethod("align",List.class).invoke(null,list);
  check(list.size()==2040,"wire indices extend through max ID");
  for(int id:new int[]{0,1,2000,2019,2020,2026,2027,2030,2039})check(ItemService.gI().getTemplate(id).id==id,"lookup by SQL ID "+id);
  check(list.get(2001).type==-1&&list.get(2001).name!=null&&list.get(2001).description!=null,"gap safe to serialize");
  try{ItemService.gI().getTemplate(2001);throw new AssertionError("gap must not be usable");}catch(IllegalArgumentException expected){}
  try{ItemService.gI().getTemplate(3000);throw new AssertionError("unknown must reject");}catch(IllegalArgumentException expected){}
  c.getMethod("align",List.class).invoke(null,list);check(list.size()==2040&&list.get(2039).id==2039,"alignment idempotent");
  System.out.println("PASS sparse SQL IDs: lookup, client indices, safe placeholders, unknown rejection");
 }
}
