package nro.models.services;

import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services_dungeon.NgocRongNamecService;

/** Forced exits only act while the character is still in the initiating zone. */
public final class DungeonExitService {
    private DungeonExitService(){}
    public static boolean exit(Player player,Zone origin,int mapId,int x,int y) {
        if(player==null || origin==null || player.zone!=origin)return false;
        synchronized(player) {
            if(player.zone!=origin)return false;
            try {
                if(player.idNRNM!=-1)NgocRongNamecService.gI().dropNamekBall(player);
                if(player.maBuHold!=null && player.maBuHold.player==player)player.maBuHold.player=null;
                if(player.maBuHold!=null || player.isMabuHold || player.effectSkill!=null && player.effectSkill.isMabuHold){
                    if(player.effectSkill!=null)EffectSkillService.gI().removeMabuHold(player);
                    player.maBuHold=null;player.isMabuHold=false;player.precentMabuHold=0;
                }
                if(player.isDie())Service.gI().hsChar(player,player.nPoint.hpMax,player.nPoint.mpMax);
                nro.models.map.Map map=MapService.gI().getMapById(mapId);
                if(map==null || map.zones.isEmpty())return false;
                Zone destination=map.zones.get(0);
                // Forced evacuation must still succeed if every home zone is full.
                for(Zone z:map.zones)if(z!=null && z.getNumOfPlayers()<z.maxPlayer){destination=z;break;}
                ChangeMapService.gI().forcedDungeonShip(player,destination,x,y);
                return player.zone!=null && player.zone!=origin && player.zone.map.mapId==mapId;
            } catch(Exception e){nro.models.utils.Logger.logException(DungeonExitService.class,e);return false;}
        }
    }
}
