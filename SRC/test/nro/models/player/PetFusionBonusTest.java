package nro.models.player;

import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player_system.Template;
import nro.models.task.TaskMain;

/** Exercises real stat calculation, including repeated refresh and master contributions. */
public final class PetFusionBonusTest {
    private static final int[] FUSIONS={6,8,9,4,10};
    public static void main(String[] args) {
        switch(args[0]) {
            case "bonus" -> bonus();
            case "stable" -> stable();
            case "legacy" -> legacy();
            case "equipment" -> equipment();
            case "max" -> maximum();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: Pet fusion "+args[0]);
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static void init(Player p,int hp,int ki,int attack,int defense,int critical,int slots) {
        p.name="Fusion fixture";p.playerIntrinsic.intrinsic=new Intrinsic();
        p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;
        p.nPoint.hpg=hp;p.nPoint.mpg=ki;p.nPoint.dameg=attack;p.nPoint.defg=defense;p.nPoint.critg=critical;
        p.nPoint.limitPower=9;p.nPoint.power=90_010_000_000L;
        for(int i=0;i<slots;i++)p.inventory.itemsBody.add(new Item());
    }
    private static Player owner(int type) {
        Player p=new Player(){@Override public void sendMessage(Message m){}};
        init(p,100_000,100_000,1_000,100,5,12);
        Pet pet=new Pet(p);init(pet,400_000,400_000,20_000,9,2,9);
        pet.typePet=(byte)type;p.pet=pet;p.nPoint.calPoint();return p;
    }
    private static void verify(Player p,int hp,int ki,int attack,int ownerHp,int ownerKi,int ownerAttack) {
        check(p.pet.nPoint.hpMax==hp,"Pet HP: expected "+hp+", actual "+p.pet.nPoint.hpMax);
        check(p.pet.nPoint.mpMax==ki,"Pet KI: expected "+ki+", actual "+p.pet.nPoint.mpMax);
        check(p.pet.nPoint.dame==attack,"Pet attack: expected "+attack+", actual "+p.pet.nPoint.dame);
        check(p.nPoint.hpMax==ownerHp && p.nPoint.mpMax==ownerKi && p.nPoint.dame==ownerAttack,"Master receives pet calculated contribution");
        check(p.nPoint.def==400 && p.nPoint.crit==5,"Pet defense and critical are not transferred or boosted");
        check(p.pet.nPoint.def==36 && p.pet.nPoint.crit==2,"Defense and critical remain unchanged");
    }
    private static void bonus() {
        for(int mode:FUSIONS)for(int type=2;type<=4;type++) {
            Player p=owner(type);p.fusion.typeFusion=(byte)mode;p.pet.status=Pet.FUSION;p.nPoint.calPoint();
            verify(p,480_000,480_000,24_000,580_000,580_000,25_000);
        }
    }
    private static void stable() {
        for(int type=2;type<=4;type++)for(int mode:FUSIONS) {
            Player p=owner(type);p.fusion.typeFusion=(byte)mode;p.pet.status=Pet.FUSION;
            for(int i=0;i<30;i++){p.nPoint.calPoint();verify(p,480_000,480_000,24_000,580_000,580_000,25_000);}
            p.fusion.typeFusion=0;p.pet.status=Pet.PROTECT;p.nPoint.calPoint();
            verify(p,400_000,400_000,20_000,100_000,100_000,1_000);
            p.fusion.typeFusion=(byte)mode;p.pet.status=Pet.FUSION;p.nPoint.calPoint();
            verify(p,480_000,480_000,24_000,580_000,580_000,25_000);
            check(p.nPoint.power==90_010_000_000L && p.pet.nPoint.power==90_010_000_000L,"Recalculation never changes strength");
        }
    }
    private static void legacy() {
        for(int type=0;type<=0;type++)for(int mode:FUSIONS) {
            Player p=owner(type);p.fusion.typeFusion=(byte)mode;p.pet.status=Pet.FUSION;
            for(int i=0;i<3;i++){p.nPoint.calPoint();verify(p,400_000,400_000,20_000,500_000,500_000,21_000);}
        }
        for(int type=2;type<=4;type++) {Player p=owner(type);verify(p,400_000,400_000,20_000,100_000,100_000,1_000);}
    }
    private static Item item(int id,int percent) {
        Item item=new Item();item.template=new Template.ItemTemplate();item.template.id=(short)id;item.template.type=0;item.quantity=1;
        for(int option:new int[]{77,103,50})item.itemOptions.add(new Item.ItemOption(new Template.ItemOptionTemplate(option,"Option #",0),percent));
        return item;
    }
    private static void equipment() {
        for(int type=2;type<=4;type++)for(int mode:new int[]{6,8,9}) {
            Player p=owner(type);p.pet.inventory.itemsBody.set(0,item(800,10));
            if(mode==8)p.inventory.itemsBag.add(item(921,25));
            if(mode==9)p.inventory.itemsBag.add(item(1819,25));
            p.fusion.typeFusion=(byte)mode;p.pet.status=Pet.FUSION;
            for(int i=0;i<5;i++) {
                p.nPoint.calPoint();int ownBase=mode==6?100_000:125_000,ownAttack=mode==6?1_000:1_250;
                verify(p,528_000,528_000,26_400,ownBase+528_000,ownBase+528_000,ownAttack+26_400);
            }
        }
        for(int type=2;type<=4;type++) {
            Player p=owner(type);
            Item ball=item(966,0);ball.template.type=11;p.pet.inventory.itemsBody.set(8,ball);
            Item outfit=item(883,0);outfit.template.type=5;p.pet.inventory.itemsBody.set(5,outfit);
            p.fusion.typeFusion=6;p.pet.status=Pet.FUSION;
            for(int i=0;i<20;i++) {
                p.nPoint.calPoint();
                verify(p,528_000,528_000,26_400,628_000,628_000,27_400);
            }
        }
    }
    private static void maximum() {
        Player p=owner(2);
        for(Player target:new Player[]{p,p.pet}) {
            target.nPoint.limitPower=9;target.nPoint.power=90_009_999_999L;
            check(target.nPoint.getPowerLimit()==90_010_000_000L && target.nPoint.getPowerNextLimit()==0,"Exact maximum tier");
            target.nPoint.powerUp(Long.MAX_VALUE);target.nPoint.powerUp(1);
            check(target.nPoint.power==90_010_000_000L,"Positive growth cannot exceed maximum");
        }
    }
}
