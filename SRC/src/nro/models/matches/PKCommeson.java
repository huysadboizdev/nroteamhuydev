package nro.models.matches;
import nro.models.player.Player;
import nro.models.services.CommesonChallengeService;
public class PKCommeson extends PVP {
 public PKCommeson(Player owner,Player clone){super(TYPE_PVP.THACH_DAU,owner,clone);}
 @Override public void finish(){started=false;}
 @Override public void update(){if(p1!=null)CommesonChallengeService.gI().update(p1);}
 @Override public void lose(Player loser,TYPE_LOSE_PVP type){if(started && loser==p1)CommesonChallengeService.gI().lose(p1);}
 @Override public void sendResult(Player loser,TYPE_LOSE_PVP type){}
 @Override public void reward(Player winner){}
 @Override public void dispose(){started=false;super.dispose();}
}
