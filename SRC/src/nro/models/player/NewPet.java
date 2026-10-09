package nro.models.player;

import nro.models.map.service.MapService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class NewPet extends Player {

    public Player master;
    public short body;
    public short leg;
    public static int idb = -310720020;

    public NewPet(Player master, short h, short b, short l) {
        this.master = master;
        this.isNewPet = true;
        this.isNewPet1 = true;
        this.id = idb;
        idb--;
        this.head = h;
        this.body = b;
        this.leg = l;
    }

    @Override
    public short getHead() {
        return head;
    }

    @Override
    public short getBody() {
        return body;
    }

    @Override
    public short getLeg() {
        return leg;
    }

    public void joinMapMaster() {
        if (!ownerVisible()) {
            return;
        }
        this.location.x = master.location.x + Util.nextInt(-10, 10);
        this.location.y = master.location.y;
        if (isPl()) {
            this.dispose();
            return;
        }
        ChangeMapService.gI().goToMap(this, master.zone);
        this.zone.load_Me_To_Another(this);
    }

    private long lastTimeMoveIdle;
    private int timeMoveIdle;
    public boolean idle;

    private void moveIdle() {
        if (idle && Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int dir = this.location.x - master.location.x <= 0 ? -1 : 1;
            PlayerService.gI().playerMove(this, master.location.x
                    + Util.nextInt(dir == -1 ? 30 : -50, dir == -1 ? 50 : 30), master.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(5000, 8000);
        }
    }

    @Override
    public void update() {
        if (master instanceof Pet && !ownerVisible()) {
            if (zone != null) {
                ChangeMapService.gI().exitMap(this);
                zone = null;
            }
            return;
        }
        super.update();
        if (this.isDie()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (master != null && master.zone != null && (this.zone == null || this.zone != master.zone) && !MapService.gI().isMapOffline(master.zone.map.mapId)) {
            joinMapMaster();
        }
        if (master != null && master.isDie()) {
            return;
        }
        moveIdle();
    }

    private boolean ownerVisible() {
        if (master == null || master.zone == null) {
            return false;
        }
        if (master instanceof Pet disciple) {
            return disciple.master != null && !disciple.isDie()
                    && disciple.getStatus() != Pet.GOHOME && disciple.getStatus() != Pet.FUSION
                    && disciple.getStatus() != Pet.HTVV && disciple.master.fusion.typeFusion == 0
                    && !MapService.gI().isMapOffline(disciple.zone.map.mapId);
        }
        return true;
    }

    public void followMaster() {
        if (!ownerVisible() || zone != master.zone) {
            return;
        }
        followMaster(50);
    }

    private void followMaster(int dis) {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= dis) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, dis);
            } else {
                this.location.x = mX + Util.nextInt(0, dis);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    @Override
    public void dispose() {
        if (zone !=null) {
            ChangeMapService.gI().exitMap(this);
        }
        this.master = null;
        super.dispose();
    }
}
