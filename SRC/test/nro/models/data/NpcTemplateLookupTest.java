package nro.models.data;
import nro.models.server.Manager;
import nro.models.player_system.Template.NpcTemplate;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
public class NpcTemplateLookupTest {
    public static void main(String[] args) {
        Manager.NPC_TEMPLATES.clear();
        NpcTemplate template=new NpcTemplate();template.id=110;template.avatar=12356;
        Manager.NPC_TEMPLATES.add(template);
        Npc npc=NpcFactory.createNPC(0,1,0,0,110);
        if(npc==null||npc.avartar!=12356)throw new AssertionError("NPC lookup shifted by gap");
        try{NpcFactory.createNPC(0,1,0,0,109);throw new AssertionError("Unknown template accepted");}
        catch(IllegalArgumentException expected){}
        System.out.println("NPC_TEMPLATE_LOOKUP=PASS sparse-ID,unknown-ID");
    }
}
