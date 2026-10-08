package nro.models.services;
import nro.models.boss.*;
import nro.models.consts.BossStatus;
import nro.models.player.*;
import nro.models.skill.PlayerSkill;
import nro.models.network.Message;
import nro.models.boss.Boss_Manager.BossManager;
import static nro.models.services.PinkFixture.*;
public final class BossFullLifecycleTest {
 public static class Probe extends Boss {
  public Probe()throws Exception{super(0,(BossData)null);}
  public int updates,leaves;public boolean fail;
  @Override public boolean noticePublic(){return false;}
  @Override public void update(){updates++;if(fail)throw new IllegalStateException("broken boss");}
  @Override public void die(Player killer){throw new IllegalStateException("reward failure");}
  @Override public void leaveMap(){leaves++;zone=null;changeStatus(BossStatus.REST);}
  public void waiting(){enterDefeatedWait();}
  public boolean deathTick(){return updateDeathLifecycle();}
 }
 public static class WhisProbe extends nro.models.boss.luyen_tap_tu_dong.Whis {
  public WhisProbe()throws Exception{super(null);}public int kills;
  @Override protected void setDie(Player p){}
  @Override public void die(Player p){kills++;}
  @Override public void chat(String s){}
 }
 public static class TrainingProbe extends nro.models.boss.luyen_tap_tu_dong.TrainingBoss {
  public TrainingProbe()throws Exception{super(nro.models.consts.BossType.PHOBAN,0,null);}
  public boolean disposed;
  @Override public void dispose(){disposed=true;}
 }
 public static class CongressProbe extends nro.models.boss.dai_hoi_vo_thuat.The23rdMartialArtCongress {
  public CongressProbe()throws Exception{super(nro.models.consts.BossType.PHOBAN,0,null);}
  @Override public void updateInfo(){} @Override public void leaveMap(){zone=null;changeStatus(BossStatus.REST);}
 }
 public static class ArenaProbe extends nro.models.boss.vo_dai_hat_mit.DeathOrAliveArena {
  public ArenaProbe()throws Exception{super(nro.models.consts.BossType.PHOBAN,0,null);}
  @Override public void updateInfo(){} @Override public void leaveMap(){zone=null;changeStatus(BossStatus.REST);}
 }
 public static class RankProbe extends nro.models.boss.sieu_hang.SuperRank {
  public RankProbe()throws Exception{super(nro.models.consts.BossType.PHOBAN,0,null);}
  @Override public void updateInfo(){} @Override public void leaveMap(){zone=null;changeStatus(BossStatus.REST);}
 }
 public static class CloneProbe extends nro.models.boss.dai_hoi_vo_thuat.ThienXinHangClone {
  public CloneProbe()throws Exception{super(0,null);}
  @Override public void updateInfo(){} @Override public void leaveMap(){zone=null;changeStatus(BossStatus.REST);}
 }
 public static class CongressMatch extends nro.models.matches.dai_hoi_vo_thuat.The23rdMartialArtCongress {
  public int next;@Override public void toTheNextRound(){next++;setTimeWait(13);}
 }
 public static class ArenaMatch extends nro.models.matches.dai_hoi_vo_thuat.DeathOrAliveArena {
  public int next;@Override public void toTheNextRound(){next++;setTimeWait(5);}
 }
 static Probe probe()throws Exception{
  Probe p=blank(Probe.class);p.isBoss=true;p.nPoint=new NPoint(p);p.nPoint.hp=100;p.nPoint.hpMax=100;p.zone=zone();p.effectSkill=new EffectSkill(p);p.effectSkin=new EffectSkin(p);p.playerSkill=new PlayerSkill(p);p.idMark=new IDMark();p.bossStatus=BossStatus.ACTIVE;p.currentLevel=0;return p;
 }
 public static void main(String[] args)throws Exception{
  install();replace(Service.class,"instance",new Service(){@Override public void charDie(Player p){}});
  replace(nro.models.services_dungeon.NgocRongNamecService.class,"instance",new nro.models.services_dungeon.NgocRongNamecService(){@Override public void dropNamekBall(Player p){}});
  Probe p=probe();try{p.injured(null,100,true,false);}catch(IllegalStateException expected){}check(p.bossStatus==BossStatus.DIE,"reward failure cannot hold dead boss in ACTIVE");
  p.waiting();check(p.bossStatus==BossStatus.AFK,"intentional death wait allowed");p.ensureDeathTransition();check(p.bossStatus==BossStatus.AFK,"intentional revival wait preserved");
  p=probe();p.nPoint.hp=0;p.ensureDeathTransition();check(p.bossStatus==BossStatus.DIE,"unexpected dead combat recovers without reward replay");
  p.prepareBom=true;p.newSkill=blank(NewSkill.class);p.newSkill.isStartSkillSpecial=true;check(p.deathTick()&&p.bossStatus==BossStatus.CHAT_E,"death phase ignores combat animation gates");
  p.deathTick();p.deathTick();check(p.leaves==1&&p.zone==null,"missing chat data still exits map");
  BossManager manager=new BossManager();Probe broken=probe(),healthy=probe();broken.fail=true;manager.addBoss(broken);manager.addBoss(healthy);manager.updateBosses();check(healthy.updates==1,"one failing boss cannot skip later bosses");check(manager.getBosses().contains(broken),"failed boss stays managed for cleanup/retry, never ghost removal");
  for(Class<? extends Boss> type:java.util.List.of(nro.models.boss.MajinBuu_12h.BuiBui.class,nro.models.boss.MajinBuu_12h.BuiBui2.class,nro.models.boss.MajinBuu_12h.Drabura.class,nro.models.boss.MajinBuu_12h.Drabura3.class,nro.models.boss.MajinBuu_12h.Yacon.class,nro.models.boss.MajinBuu_12h.Goku.class,nro.models.boss.MajinBuu_12h.Cadic.class,TrainingProbe.class,nro.models.boss.luyen_tap_tu_dong.Whis.class)){
   Boss actual=blank(type);actual.nPoint=blank(NPoint.class);actual.bossStatus=BossStatus.DIE;
   try{actual.die(null);}catch(Exception optionalSideEffect){}
   check(actual.bossStatus==BossStatus.AFK,type.getSimpleName()+" intended post-defeat wait retained");actual.ensureDeathTransition();check(actual.bossStatus==BossStatus.AFK,"recovery does not bypass intentional timer");
  }
  WhisProbe whis=blank(WhisProbe.class);whis.nPoint=new NPoint(whis);whis.nPoint.hp=100;whis.nPoint.hpMax=100;
  var level=nro.models.boss.luyen_tap_tu_dong.Whis.class.getDeclaredField("level");level.setAccessible(true);level.setLong(whis,1);
  Player whisOwner=player(90);whis.playerAtt=whisOwner;
  whis.injured(whisOwner,40,true,false);check(whis.nPoint.hp==60&&whis.kills==0,"Whis cannot die at half remaining HP");whis.injured(whisOwner,60,true,false);whis.injured(whisOwner,40,true,false);check(whis.kills==1,"Whis dies at zero once, repeat hits do not reward again");
  Probe owner=probe();owner.currentLevel=0;owner.bossAppearTogether=new Boss[][]{new Boss[]{null}};
  Boss child=blank(nro.models.boss.tieu_doi_sat_thu.SO1.class);var parent=Boss.class.getDeclaredField("parentBoss");parent.setAccessible(true);parent.set(child,owner);child.doneChatE();check(owner.bossStatus==BossStatus.ACTIVE,"null companion slots skipped");owner.currentLevel=9;child.doneChatE();check(owner.getCompanionBosses().length==0,"late callback after form advance safely ignored");
  for(Class<? extends Boss> type:java.util.List.of(CongressProbe.class,ArenaProbe.class,RankProbe.class,CloneProbe.class)){
   Boss special=blank(type);special.nPoint=new NPoint(special);special.zone=zone();special.bossStatus=BossStatus.DIE;special.effectSkill=new EffectSkill(special);special.newSkill=blank(NewSkill.class);special.newSkill.isStartSkillSpecial=true;
   special.update();check(special.bossStatus==BossStatus.CHAT_E,type.getSimpleName()+" custom update cannot skip death phase");special.update();special.update();check(special.zone==null&&special.bossStatus==BossStatus.REST,"custom update performs terminal cleanup despite skill flags");
  }
  Probe defeated=probe();defeated.nPoint.hp=0;defeated.ensureDeathTransition();defeated.dispose();check(defeated.nPoint==null,"real disposal clears HP object before match tick");
  Owner challenger=player(90);challenger.zone=zone();challenger.nPoint=new NPoint(challenger);challenger.nPoint.hp=100;challenger.location=new Location();challenger.isPKDHVT=true;
  CongressMatch congress=new CongressMatch();congress.setPlayer(challenger);congress.setBoss(defeated);congress.setZone(challenger.zone);congress.setTime(100);congress.update();check(congress.next==1&&challenger.levelWoodChest==1,"Congress match records disposed boss victory/reward");
  defeated=probe();defeated.nPoint.hp=0;defeated.ensureDeathTransition();defeated.dispose();ArenaMatch arena=new ArenaMatch();arena.setPlayer(challenger);arena.setBoss(defeated);arena.setZone(challenger.zone);arena.setTime(100);arena.update();check(arena.next==1,"HatMit match records disposed boss victory");
  TrainingProbe training=blank(TrainingProbe.class);training.nPoint=new NPoint(training);training.nPoint.hpMax=100;training.zone=zone();training.playerAtt=player(91);training.bossStatus=BossStatus.AFK;
  training.effectSkill=new EffectSkill(training);training.effectSkin=new EffectSkin(training);training.idMark=new IDMark();
  var originalZone=training.zone;final nro.models.map.Zone[] lookup={null};
  replace(nro.models.services_dungeon.TrainingService.class,"instance",new nro.models.services_dungeon.TrainingService(){
   @Override public Player getNonInteractiveNPC(nro.models.map.Zone z,int id){lookup[0]=z;return null;}
   @Override public void luyenTapEnd(Player p,int id){throw new IllegalStateException("optional NPC notification failed");}
  });
  replace(Service.class,"instance",new Service(){@Override public void sendMessAnotherNotMeInMap(Player p,Message msg){}});
  nro.models.boss.Boss_Manager.OtherBossManager.gI().addBoss(training);
  training.leaveMap();check(lookup[0]==originalZone,"training NPC looked up before zone is cleared");check(training.zone==null&&training.disposed&&training.bossStatus==BossStatus.REST,"optional training notification failure cannot block exit/disposal");check(!nro.models.boss.Boss_Manager.OtherBossManager.gI().getBosses().contains(training),"disposed training boss removed from manager");
  System.out.println("PASS full boss lifecycle: reward failure, intended AFK, zero-HP recovery, terminal gates, invalid chats, manager failure isolation, training cleanup");
 }
}
