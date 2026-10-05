package nro.models.player;

import lombok.Getter;
import lombok.Setter;
import nro.models.player.Player;

/**
 * @author By Mr Blue
 */
@Setter
@Getter
public class PlayerEvent {
    private int eventPoint;
    private Player player;
    public int luotNhanNgocMienPhi = 1;
    public int luotNhanCapsuleBang = 1;

    public PlayerEvent(Player player) {
        this.player = player;
    }
    
    public void addEventPoint(int num) {
        synchronized(player) { eventPoint = Math.addExact(eventPoint,num); if(num>0)nro.models.task.BadgesTaskService.updateCountBagesTask(player,15,num); }
    }
    
    public void subEventPoint(int num) {
        synchronized(player) { eventPoint = Math.subtractExact(eventPoint,num); }
    }

    public int getEventPoint() { synchronized(player) { return eventPoint; } }
    public void setEventPoint(int value) { synchronized(player) { eventPoint=value; } }
    public void update() {
       
    }

}
