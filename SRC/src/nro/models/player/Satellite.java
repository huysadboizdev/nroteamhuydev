package nro.models.player;

import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Satellite {

    public volatile boolean isHP;
    public volatile boolean isMP;
    public volatile boolean isIntelligent;
    public volatile boolean isDefend;
    public volatile long lastHPTime;
    public volatile long lastMPTime;
    public volatile long lastIntelligentTime;
    public volatile long lastDefendTime;

    public synchronized void update() {
        if (isHP && Util.canDoWithTime(lastHPTime, 3000)) {
            isHP = false;
        }
        if (isMP && Util.canDoWithTime(lastMPTime, 3000)) {
            isMP = false;
        }
        if (isIntelligent && Util.canDoWithTime(lastIntelligentTime, 3000)) {
            isIntelligent = false;
        }
        if (isDefend && Util.canDoWithTime(lastDefendTime, 3000)) {
            isDefend = false;
        }
    }
}
