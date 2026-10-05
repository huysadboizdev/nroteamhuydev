package nro.models.commeson;

import java.time.Instant;
import java.time.ZoneOffset;

/** Durable character-bound attempt. All deadlines are absolute epoch milliseconds. */
public final class CommesonAttempt {
    public enum State { RESERVED, FIGHT, WON, PICKING, PICKED, RETURN, DONE }
    public final String matchId;
    public final long playerId;
    public final String attemptDay;
    public State state;
    public long deadline;
    public CommesonAttempt(String id,long player,long now) {
        matchId=id;playerId=player;attemptDay=day(now);state=State.FIGHT;deadline=now+300_000;
    }
    public CommesonAttempt(String id,long player,String day,State state,long deadline) {
        matchId=id;playerId=player;attemptDay=day;this.state=state;this.deadline=deadline;
    }
    public static String day(long now) {return Instant.ofEpochMilli(now).atOffset(ZoneOffset.ofHours(7)).toLocalDate().toString();}
    public boolean due(long now) {return state!=State.DONE && now>=deadline;}
    public boolean win(long now) {if(state!=State.FIGHT || due(now))return false;state=State.WON;deadline=now+60_000;return true;}
    public boolean pick(long now) {if(state!=State.WON || due(now))return false;state=State.PICKED;deadline=now+60_000;return true;}
}
