package nro.models.services;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import nro.models.player.Player;
import nro.models.mob.Mob;
import nro.models.server.Manager;

/** Direct-hit observation only; admission never touches a player's chat or wallet. */
public final class HighDamageNoticeService {
    private static final long THRESHOLD=20_000_000L, PLAYER_DELAY=5_000, GLOBAL_DELAY=1_000;
    private static final int MAX_PLAYERS=16;
    private static class Holder {
        static final HighDamageNoticeService INSTANCE=new HighDamageNoticeService(
                ()->System.nanoTime()/1_000_000,ChatGlobalService::sendSystemNotice,
                text->Service.gI().sendThongBaoAllPlayer(text));
    }
    public static HighDamageNoticeService gI() { return Holder.INSTANCE; }
    private final LongSupplier clock;
    private final Consumer<String> world,info;
    private final Map<Long,Long> players=new HashMap<>();
    private long globalAt;
    private boolean hasGlobal;
    public HighDamageNoticeService(LongSupplier clock,Consumer<String> world,Consumer<String> info) {
        this.clock=clock;this.world=world;this.info=info;
    }
    private static boolean real(Player p) { return p!=null&&p.isPl(); }
    private static String clean(String label) {
        if(label==null)return "";
        return label.replaceAll("\\|[^|]*\\|", "").replaceAll("[\\p{Cc}\\p{Cf}|]", " ").replaceAll("\\s+", " ").trim();
    }
    private static String shorten(String s,int max) {
        if(s.length()<=max)return s;
        int end=Math.max(0,max-1);
        if(end>0&&Character.isHighSurrogate(s.charAt(end-1)))end--;
        return s.substring(0,end)+"…";
    }
    static String format(String player,String skill,long damage) {
        String value=String.format(java.util.Locale.ROOT,"%,d",damage).replace(',','.');
        String a="Người chơi ",b=" dùng chiêu ",c=" với mức sát thương là "+value+".";
        player=clean(player);skill=clean(skill);
        if(player.isEmpty())player="Ẩn danh";
        int available=100-a.length()-b.length()-c.length();
        int playerSize=Math.min(player.length(),Math.max(1,available/2));
        int skillSize=Math.min(skill.length(),available-playerSize);
        playerSize=Math.min(player.length(),available-skillSize);
        return a+shorten(player,playerSize)+b+shorten(skill,skillSize)+c;
    }
    void report(Player player,String skill,long damage) {
        if(!real(player))return;
        report(player.id,player.name,skill,damage);
    }
    private void report(long id,String name,String skill,long damage) {
        if(damage<=THRESHOLD||clean(skill).isEmpty())return;
        String text=format(name,skill,damage);
        synchronized(this) {
            long now=clock.getAsLong();
            players.entrySet().removeIf(e->now-e.getValue()>=PLAYER_DELAY);
            Long last=players.get(id);
            if((last!=null&&now-last<PLAYER_DELAY)||(hasGlobal&&now-globalAt<GLOBAL_DELAY))return;
            // At one accepted event per second, at most five unexpired players can exist.
            if(players.size()>=MAX_PLAYERS)return;
            players.put(id,now);globalAt=now;hasGlobal=true;
        }
        try { world.accept(text); } catch(Exception ignored) { }
        try { info.accept(text); } catch(Exception ignored) { }
    }
    private static String skillName(int id) {
        try {
        for(var nclass:Manager.NCLASS) {
            if(nclass==null||nclass.skillTemplatess==null)continue;
            var template=nclass.getSkillTemplate(id);
            if(template!=null)return template.name;
        }
        } catch(Exception unavailable) { return null; }
        return null;
    }
    private static final ThreadLocal<Scope> HIT=new ThreadLocal<>();
    private record Notice(long playerId,String name,String skill,long damage) {}
    /** Records inside combat; sends only after injured() has released its monitor. */
    public static final class Scope implements AutoCloseable {
        private final Scope previous;
        private final Object target;
        private final Player attacker;
        private final long playerId;
        private final String name,skill;
        private long damage;
        private Notice pending;
        private boolean closed;
        private Scope(Player player,Object target,Integer skillId) {
            previous=HIT.get();this.target=target;this.attacker=player;
            boolean valid=real(player)&&skillId!=null&&skillId>=0;
            playerId=valid?player.id:0;name=valid?player.name:null;
            skill=valid?skillName(skillId):null;
            HIT.set(this);
        }
        @Override public void close() {
            if(closed)return;closed=true;
            if(previous==null)HIT.remove();else HIT.set(previous);
            Notice notice=pending!=null?pending:damage>THRESHOLD&&skill!=null?new Notice(playerId,name,skill,damage):null;
            if(notice!=null) {
                if(previous!=null) {
                    if(previous.pending==null)previous.pending=notice;
                } else {
                    try { gI().report(notice.playerId,notice.name,notice.skill,notice.damage); } catch(Exception ignored) { }
                }
            }
        }
    }
    public static Scope begin(Player player,Object target,Integer skillId) { return new Scope(player,target,skillId); }
    public static void hpRemoved(Object target,long damage) {
        Scope scope=HIT.get();
        if(scope!=null&&scope.target==target&&scope.damage==0&&damage>THRESHOLD)scope.damage=damage;
    }
    public static void hpRemoved(Player attacker,Object target,long damage) {
        Scope scope=HIT.get();
        if(scope!=null&&attacker!=null&&scope.attacker==attacker)hpRemoved(target,damage);
    }
    public static void injureMob(Mob target,Player attacker,long damage,boolean dieWhenHpFull,int skillId) {
        try(Scope scope=begin(attacker,target,skillId)) { target.injured(attacker,damage,dieWhenHpFull); }
    }
    public static int selectedSkill(Player p) {
        return p!=null&&p.playerSkill!=null&&p.playerSkill.skillSelect!=null&&p.playerSkill.skillSelect.template!=null
                ?p.playerSkill.skillSelect.template.id:-1;
    }
}
