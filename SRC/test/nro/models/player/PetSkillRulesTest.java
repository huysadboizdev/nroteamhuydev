package nro.models.player;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import nro.models.network.Message;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.skill.*;
import nro.models.utils.SkillUtil;

/** Offline real packet and execution regressions; compatible with the old JAR for RED. */
public final class PetSkillRulesTest {
    private static final long LIMIT=60_000_000_000L;
    private static final int[] FIFTH={24,26,25};
    private static class Owner extends Player {
        byte[] packet;
        @Override public void sendMessage(Message msg){if(msg.command==-107)packet=msg.getData();}
    }
    private static final Service WIRE=new Service(){@Override public String getCurrStrLevel(Player p){return "test";}};
    public static void main(String[] args)throws Exception {
        setup();
        switch(args[0]) {
            case "packets" -> packets();case "thresholds" -> thresholds();case "direct" -> direct();
            case "legacy" -> legacy();case "short" -> shortLists();case "execution" -> execution();
            case "load" -> load();case "use-fifth" -> useFifth();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: Pet skill rules "+args[0]);
    }
    private static void setup()throws Exception {
        for(int g=0;g<3;g++)Manager.NCLASS.add(new NClass());
        for(int id=0;id<=26;id++) {
            Template.SkillTemplate t=new Template.SkillTemplate();t.id=(byte)id;t.type=3;t.name="test";
            Skill s=new Skill();s.template=t;s.point=1;s.skillId=(short)(100+id);s.manaUse=0;s.coolDown=1000;
            t.skillss.add(s);Manager.NCLASS.get(0).skillTemplatess.add(t);
        }
    }
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static Pet pet(int type,int gender,long power,int size) {
        Owner p=new Owner();Pet pet=new Pet(p);p.pet=pet;pet.name="test";pet.gender=(byte)gender;
        pet.typePet=(byte)type;pet.nPoint.power=power;pet.nPoint.limitPower=6;pet.nPoint.mp=1000;
        for(int i=0;i<size;i++)pet.playerSkill.skills.add(i<4?SkillUtil.createSkill(i*2,1):SkillUtil.createEmptySkill());
        return pet;
    }
    private static Object invoke(Pet p,String method,Class<?>[] types,Object... args)throws Exception {
        Method m=Pet.class.getDeclaredMethod(method,types);m.setAccessible(true);
        try{return m.invoke(p,args);}catch(InvocationTargetException e){throw new AssertionError(method+" must be safe",e.getCause());}
    }
    private static Object invoke(Pet p,String method)throws Exception{return invoke(p,method,new Class<?>[0]);}
    private static void packet(Pet pet,String file)throws Exception {
        Owner owner=(Owner)pet.master;WIRE.showInfoPet(owner);check(owner.packet!=null,"Pet packet sent");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(owner.packet));
        check(in.readByte()==2,"Pet info subtype");in.readShort();int body=in.readUnsignedByte();
        for(int i=0;i<body;i++)check(in.readShort()==-1,"Empty fixture body");
        for(int i=0;i<5;i++)in.readInt();in.readUTF();in.readUTF();in.readLong();in.readLong();
        in.readByte();in.readShort();in.readShort();in.readByte();in.readShort();
        int expected=pet.typePet>=2&&pet.typePet<=4?5:4;
        check(in.readUnsignedByte()==expected,"Type "+pet.typePet+" must advertise "+expected+" skills");
        for(int i=0;i<expected;i++) {
            short id=in.readShort();
            if(id==-1){String lock=in.readUTF();if(i==4)check(lock.contains("60"),"Fifth placeholder requires 60b");}
            if(i==4&&pet.nPoint.power<LIMIT)check(id==-1,"Legacy fifth stays locked below60b");
        }
        check(in.available()==0,"Declared skill count consumes entire payload; leftover="+in.available());
        if(file!=null){Path dir=Path.of(".analysis/pet-skill-plan/packets");Files.createDirectories(dir);Files.write(dir.resolve(file+".bin"),owner.packet);}
    }
    private static void packets()throws Exception {
        packet(pet(0,0,LIMIT,7),"normal");packet(pet(1,0,LIMIT,7),"mabu");
        for(int t=2;t<=4;t++)for(int size:new int[]{0,4,5,7})packet(pet(t,0,40_000_000_000L,size),t==2&&size==7?"uub-locked":null);
        Pet open=pet(2,0,LIMIT,7);open.openSkill5();packet(open,"uub-open");
    }
    private static void thresholds()throws Exception {
        Pet surplus=pet(2,0,LIMIT,7);
        Skill savedSurplus=SkillUtil.createSkill(0,1);
        surplus.playerSkill.skills.set(5,savedSurplus);
        invoke(surplus,"updatePower");
        check(surplus.playerSkill.skills.get(4).template != null
                && surplus.playerSkill.skills.get(4).template.id==24,"Learned surplus slot cannot block automatic fifth unlock");
        check(surplus.playerSkill.skills.get(5)==savedSurplus,"Automatic unlock retains learned surplus data");
        for(int t=2;t<=4;t++)for(int g=0;g<3;g++)for(long power:new long[]{40_000_000_000L,LIMIT-1,LIMIT,LIMIT+1}) {
            Pet p=pet(t,g,power,7);invoke(p,"updatePower");Skill s=p.playerSkill.skills.get(4);
            check(power<LIMIT?s.skillId==-1:s.template.id==FIFTH[g],"Automatic fifth obeys60b, type="+t+" gender="+g+" power="+power);
            check(p.nPoint.limitPower==6&&p.nPoint.power==power,"No free power tier or power changes");
        }
    }
    private static void direct() {
        for(int t=0;t<=5;t++)for(int g=0;g<3;g++)for(long power:new long[]{40_000_000_000L,LIMIT-1,LIMIT,LIMIT+1}) {
            Pet p=pet(t,g,power,7);p.openSkill5();Skill s=p.playerSkill.skills.get(4);
            boolean allowed=t>=2&&t<=4&&power>=LIMIT;
            check(allowed?s.template!=null&&s.template.id==FIFTH[g]:s.skillId==-1,"Direct open rejects type/power bypass");
            if(allowed){s.point=7;s.coolDown=9876;p.openSkill5();check(p.playerSkill.skills.get(4)==s&&s.point==7,"Repeated open preserves learned legacy skill");}
        }
    }
    private static void legacy()throws Exception {
        for(int t=0;t<=4;t++)for(long power:new long[]{40_000_000_000L,LIMIT-1,LIMIT,LIMIT+1}) {
            Pet p=pet(t,0,power,7);Skill old=SkillUtil.createSkill(24,1);old.point=7;old.coolDown=9876;p.playerSkill.skills.set(4,old);
            packet(p,null);if(power<LIMIT||t<2){
                Skill selected=p.playerSkill.skills.get(0);p.playerSkill.skillSelect=selected;
                check(!(boolean)invoke(p,"useSkill5"),"Legacy fifth execution denied");
                check(p.playerSkill.skillSelect==selected,"Denied type/power leaves selected skill untouched");
            }
            p.openSkill5();check(p.playerSkill.skills.get(4)==old&&old.point==7&&old.coolDown==9876,"Stored skill retained");
        }
    }
    private static void shortLists()throws Exception {
        Pet missing=pet(2,0,LIMIT,4);Skill selected=missing.playerSkill.skills.get(0);
        missing.playerSkill.skillSelect=selected;
        check(!(boolean)invoke(missing,"useSkill5"),"Missing fifth safely refuses execution");
        check(missing.playerSkill.skillSelect==selected,"Missing fifth preserves selected skill");
        for(int t=0;t<=4;t++)for(int size:new int[]{0,1,4,5,7}) {
            Pet p=pet(t,0,LIMIT,size);for(int index:new int[]{-1,0,5,8}) {
                Object s=invoke(p,"getSkill",new Class<?>[]{int.class},index);
                if(index<1||index>size||index>(t>=2?5:4))check(s==null,"Unavailable slot safely returns null");
            }
            p.openSkill5();if(t>=2)check(p.playerSkill.skills.size()>=5&&p.playerSkill.skills.get(4).template.id==24,"Short special list safely padded/opened");
            check(p.playerSkill.skills.size()>=size,"Surplus saved slots retained");
        }
    }
    private static void execution()throws Exception {
        final int[] casts={0};SkillService service=new SkillService(){@Override public void useSkillAlone(Player p){casts[0]++;}};
        for(int t=0;t<=5;t++)for(long power:new long[]{40_000_000_000L,LIMIT-1,LIMIT,LIMIT+1})for(int slot:new int[]{0,3,4,6})for(int id:new int[]{24,25,26}) {
            Pet p=pet(t,0,power,7);Skill skill=SkillUtil.createSkill(id,1);p.playerSkill.skills.set(slot,skill);p.playerSkill.skillSelect=skill;
            boolean allowed=t>=2&&t<=4&&power>=LIMIT&&slot==4;
            check(service.canUseSkillWithCooldown(p)==allowed,"Cooldown gate blocks injected fifth type/power/slot");
            check(service.canUseSkillWithMana(p)==allowed,"Mana gate blocks injected fifth type/power/slot");
            int before=casts[0];check(service.useSkill(p,null,null,-1,null)==allowed,"Central execution enforces fifth authorization");
            check(casts[0]==before+(allowed?1:0),"Rejected selection cannot execute");
        }
        Pet p=pet(2,0,LIMIT,7);Skill detached=SkillUtil.createSkill(24,1);p.playerSkill.skillSelect=detached;
        check(!service.useSkill(p,null,null,-1,null),"Detached selected skill rejected");
        p.playerSkill.skills.set(4,detached);p.playerSkill.skills.set(0,detached);
        check(!service.useSkill(p,null,null,-1,null),"Same fifth injected into another slot rejected");
        p.playerSkill.skillSelect=null;check(!service.useSkill(p,null,null,-1,null),"Null selection fails closed");
        p.playerSkill.skillSelect=SkillUtil.createEmptySkill();check(!service.useSkill(p,null,null,-1,null),"Empty selection fails closed");
        p.playerSkill.skills.set(0,SkillUtil.createSkill(0,1));p.playerSkill.skillSelect=p.playerSkill.skills.get(0);
        check(service.canUseSkillWithMana(p)&&service.canUseSkillWithCooldown(p),"Regular attack remains usable");
    }
    private static void load()throws Exception {
        Class<?> rules;
        try{rules=Class.forName("nro.models.player.PetSkillRules");}
        catch(ClassNotFoundException e){throw new AssertionError("Shared load normalization policy missing",e);}
        Method ensure=rules.getMethod("ensureSlots",Pet.class);
        for(int t=0;t<=5;t++)for(int size:new int[]{0,1,4,5,7}) {
            Pet p=pet(t,0,40_000_000_000L,size);List<Skill> saved=new ArrayList<>(p.playerSkill.skills);
            ensure.invoke(null,p);ensure.invoke(null,p);
            check(p.playerSkill.skills.size()==Math.max(size,t>=2&&t<=4?5:4),"Load pads all special types including Uub; retains surplus");
            for(int i=0;i<size;i++)check(p.playerSkill.skills.get(i)==saved.get(i),"Relog/load retains every saved skill object");
            check(p.nPoint.limitPower==6&&p.nPoint.power==40_000_000_000L,"Load skill padding leaves paid tiers/power unchanged");
        }
    }
    private static void useFifth()throws Exception {
        int[] effects={0};Field instance=SkillService.class.getDeclaredField("instance");instance.setAccessible(true);
        instance.set(null,new SkillService(){
            @Override public void newSkillNotFocus(Player p,int status){effects[0]++;}
            @Override public void affterUseSkill(Player p,int id){effects[0]++;}
        });
        for(int t=0;t<=5;t++)for(int g=0;g<3;g++)for(long power:new long[]{40_000_000_000L,LIMIT-1,LIMIT,LIMIT+1}) {
            Pet p=pet(t,g,power,7);Skill fifth=SkillUtil.createSkill(FIFTH[g],1);p.playerSkill.skills.set(4,fifth);
            p.zone=new Zone(null,0,20){
                @Override public Player findNearestPlayer(Pet pet){return null;}
                @Override public Mob findNearestMob(Pet pet){return new Mob();}
            };
            p.newSkill=new NewSkill(p){@Override public synchronized void setSkillSpecial(byte d,short x,short y,short tx,short ty){effects[0]++;}};
            Skill selected=p.playerSkill.skills.get(0);p.playerSkill.skillSelect=selected;int before=effects[0];
            boolean allowed=t>=2&&t<=4&&power>=LIMIT;
            check((boolean)invoke(p,"useSkill5")==allowed,"Real useSkill5 gates type/power and accepts every planet at60b");
            check(effects[0]==before+(allowed?3:0),"Locked fifth cannot emit damage/skill effects");
            if(!allowed)check(p.playerSkill.skillSelect==selected,"Rejected useSkill5 keeps original selection");
        }
    }
}
