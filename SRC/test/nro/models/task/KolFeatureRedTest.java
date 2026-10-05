package nro.models.task;
public class KolFeatureRedTest {
 public static void main(String[] args) throws Exception {
 for(String name: new String[]{"nro.models.player.KolQuestState","nro.models.task.KolQuestCatalog","nro.models.services.KolQuestService","nro.models.database.KolRewardDAO"}) {
 try {Class.forName(name);} catch(ClassNotFoundException e){throw new AssertionError("Missing required KOL v2 behavior: "+name);}
 }
 }
}