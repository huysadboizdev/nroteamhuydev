package nro.models.boss.notice;

/** Public boss data deliberately has no zone, coordinates or private instance location. */
public record BossNoticeRow(String spawnId,String serverRun,long bossId,int form,String bossName,
        String mapName,long appearedAt,long defeatedAt,String killerName,String status,
        long nextSpawnAt,String etaKind,long version,long updatedAt) {
    public BossNoticeRow change(String map,long death,String killer,String state,long next,String eta,long now){
        return new BossNoticeRow(spawnId,serverRun,bossId,form,bossName,map,appearedAt,death,killer,state,next,eta,version+1,now);
    }
}
