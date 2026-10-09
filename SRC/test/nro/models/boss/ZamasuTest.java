package nro.models.boss;
import java.util.Arrays;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.AppearType;
public final class ZamasuTest {
    static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    public static void main(String[] args){try{
        int id=BossID.class.getField("ZAMASU").getInt(null);
        BossData z=(BossData)BossesData.class.getField("ZAMASU").get(null),s=BossesData.SUPER_BLACK_GOKU;
        check(z.getName().equals("Zamasu"),"exact display name");
        check(z.getDame()==s.getDame()&&Arrays.equals(z.getHp(),s.getHp()),"Super Black Goku stats");
        check(Arrays.deepEquals(z.getSkillTemp(),s.getSkillTemp()),"all skills, levels and cooldowns match");
        check(Arrays.equals(z.getMapJoin(),s.getMapJoin()),"same future maps");
        check(Arrays.equals(z.getOutfit(),new short[]{903,904,905,-1,-1,-1}),"Zamasu costume");
        check(z.getTypeAppear()==AppearType.DEFAULT_APPEAR&&z.getSecondsRest()==300,"independent five-minute respawn");
        check(z.getHp()!=s.getHp()&&z.getMapJoin()!=s.getMapJoin()&&z.getSkillTemp()!=s.getSkillTemp(),"independent mutable template arrays");
        for(int i=0;i<z.getSkillTemp().length;i++)check(z.getSkillTemp()[i]!=s.getSkillTemp()[i],"independent skill row");
        Boss boss=BossManager.gI().createBoss(id);check(boss!=null&&boss.id==id&&boss.data.length==1,"factory creates independent single-stage boss");
        boss.currentLevel=0;check(boss.getHead()==903&&boss.getBody()==904&&boss.getLeg()==905,"actual appearance getters");
        var name=boss.getClass().getDeclaredMethod("getSpawnName");name.setAccessible(true);
        for(int i=0;i<50;i++)check(name.invoke(boss).equals("Zamasu"),"no random display suffix");
        check(BossesData.BLACK_GOKU.getName().equals("Black Goku")&&BossesData.SUPER_BLACK_GOKU.getTypeAppear()==AppearType.ANOTHER_LEVEL,"original Black Goku stages unchanged");
        System.out.println("ZAMASU PASS stats/skills/maps/appearance/factory/identity/rest/isolated-data");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
