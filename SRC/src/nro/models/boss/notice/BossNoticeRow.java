package nro.models.boss.notice;
/** Internal snapshot retains the zone; public packets mask it while retaining their layout. */
public record BossNoticeRow(String spawnId,String serverRun,long bossId,int form,String bossName,
 String mapName,long appearedAt,long defeatedAt,String killerName,String status,
 long nextSpawnAt,String etaKind,long version,long updatedAt,
 int category,int semanticBossId,boolean questSupport,int zoneId,int schemaVersion) {
 public BossNoticeRow(String spawnId,String serverRun,long bossId,int form,String bossName,String mapName,long appearedAt,long defeatedAt,String killerName,String status,long nextSpawnAt,String etaKind,long version,long updatedAt){
  this(spawnId,serverRun,bossId,form,bossName,mapName,appearedAt,defeatedAt,killerName,status,nextSpawnAt,etaKind,version,updatedAt,BossNoticeCatalog.legacyCategory(bossId,form),(int)bossId,false,-1,1);
 }
 public BossNoticeRow change(String map,long death,String killer,String state,long next,String eta,long now){
  return new BossNoticeRow(spawnId,serverRun,bossId,form,bossName,map,appearedAt,death,killer,state,next,eta,version+1,now,category,semanticBossId,questSupport,zoneId,schemaVersion);
 }
 public BossNoticeRow location(String map,int zone,long now){
  return new BossNoticeRow(spawnId,serverRun,bossId,form,bossName,map,appearedAt,defeatedAt,killerName,status,nextSpawnAt,etaKind,version+1,now,category,semanticBossId,questSupport,zone,schemaVersion);
 }
}
