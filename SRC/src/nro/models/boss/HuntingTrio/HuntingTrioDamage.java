package nro.models.boss.HuntingTrio;

import nro.models.player.Player;

/** Adjust at the actual target, after hit qualification; never mutate the boss's shared dame. */
public final class HuntingTrioDamage {
    private HuntingTrioDamage(){}
    public static long percent(int hpMax) { return hpMax<=0?0:Math.max(1,(long)hpMax*5/100); }
    public static long damageFor(Player attacker,Player target,long original) {
        if(original<=0||!(attacker instanceof HuntingTrioBoss boss)||!boss.combatReady()
                ||target==null||target==attacker||target.isBoss||target.beforeDispose||target.nPoint==null
                ||target.isDie()||attacker.zone==null||target.zone!=attacker.zone)return 0;
        return nro.models.services.BossAdminConfig.nativeDamage(boss, (int) percent(target.nPoint.hpMax));
    }
}
