package nro.models.services;

import java.sql.*;
import java.time.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.matches.TOP;

/** Serializes commits and publishes immutable snapshots only after SQL success. */
public final class WhisWeeklyService {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private static volatile WhisWeeklyService instance = new WhisWeeklyService(LocalManager::getConnection,Clock.systemUTC());
    public static WhisWeeklyService gI() { return instance; }
    private final Connections connections;
    private final Clock clock;
    private record Snapshot(LocalDate week,List<TOP> top,Map<Long,Integer> rewards) {}
    private volatile Snapshot snapshot = new Snapshot(LocalDate.MIN,List.of(),Map.of());
    public WhisWeeklyService(Connections connections,Clock clock) { this.connections=connections;this.clock=clock; }
    public List<TOP> top() {
        Snapshot s=snapshot;
        return s.week.equals(WhisWeeklyPolicy.weekStart(clock.millis())) ? s.top : List.of();
    }
    public int bonusPercent(long playerId) {
        Snapshot s=snapshot;
        return s.week.equals(WhisWeeklyPolicy.weekStart(clock.millis())) ? s.rewards.getOrDefault(playerId,0) : 0;
    }
    public synchronized void refresh() throws SQLException {
        long now=clock.millis();LocalDate week=WhisWeeklyPolicy.weekStart(now);
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                advanceWeek(c,week);
                Snapshot next=readSnapshot(c,week);
                c.commit();snapshot=next;
            } catch(SQLException|RuntimeException e) { c.rollback();throw e; }
        }
    }
    /** Caller holds owner lock, matching PlayerDAO, until the in-memory mirror is updated. */
    public synchronized void recordWin(Player owner,int level,int elapsed) throws SQLException {
        if(owner==null || level<1) throw new IllegalArgumentException("Invalid Whis victory");
        long now=clock.millis();LocalDate week=WhisWeeklyPolicy.weekStart(now);
        int nextLevel=Math.max(owner.traning.getTop(),level);
        int nextWins=owner.thachdauwhis==Integer.MAX_VALUE?Integer.MAX_VALUE:owner.thachdauwhis+1;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                advanceWeek(c,week);
                try(PreparedStatement p=c.prepareStatement("INSERT INTO whis_weekly_score(week_start,player_id,wins,last_win) VALUES(?,?,1,?) ON DUPLICATE KEY UPDATE wins=LEAST(2147483647,wins+1),last_win=VALUES(last_win)")) {
                    p.setDate(1,java.sql.Date.valueOf(week));p.setLong(2,owner.id);p.setLong(3,now);p.executeUpdate();
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE player SET thachdauwhis=?,data_luyentap=JSON_SET(data_luyentap,'$[5]',?,'$[6]',?,'$[7]',?) WHERE id=?")) {
                    p.setInt(1,nextWins);p.setInt(2,nextLevel);p.setInt(3,Math.max(0,elapsed));p.setLong(4,now);p.setLong(5,owner.id);
                    if(p.executeUpdate()!=1)throw new SQLException("Whis owner not found");
                }
                Snapshot next=readSnapshot(c,week);
                c.commit();snapshot=next;
                owner.thachdauwhis=nextWins;owner.traning.setTop(nextLevel);
                owner.traning.setTime(Math.max(0,elapsed));owner.traning.setLastTime(now);
            } catch(SQLException|RuntimeException e) { c.rollback();throw e; }
        }
    }
    private void advanceWeek(Connection c,LocalDate week) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO whis_weekly_state(id,week_start) VALUES(1,?)")) {
            p.setDate(1,java.sql.Date.valueOf(week));p.executeUpdate();
        }
        LocalDate stored;
        try(Statement p=c.createStatement();ResultSet r=p.executeQuery("SELECT week_start FROM whis_weekly_state WHERE id=1 FOR UPDATE")) {
            if(!r.next())throw new SQLException("Missing Whis week state");stored=r.getDate(1).toLocalDate();
        }
        if(stored.isAfter(week))throw new SQLException("Whis clock moved backwards");
        if(stored.equals(week))return;
        // Only the immediately preceding calendar week can reward the current week.
        try(PreparedStatement p=c.prepareStatement("SELECT player_id FROM whis_weekly_score WHERE week_start=? AND wins>0 ORDER BY wins DESC,last_win ASC,player_id ASC LIMIT 3")) {
            p.setDate(1,java.sql.Date.valueOf(week.minusWeeks(1)));
            try(ResultSet r=p.executeQuery()) {
                int rank=0;
                while(r.next()) {
                    try(PreparedStatement award=c.prepareStatement("INSERT INTO whis_weekly_reward(reward_week,player_id,rank_no) VALUES(?,?,?)")) {
                        award.setDate(1,java.sql.Date.valueOf(week));award.setLong(2,r.getLong(1));award.setInt(3,++rank);award.executeUpdate();
                    }
                }
            }
        }
        try(PreparedStatement p=c.prepareStatement("UPDATE whis_weekly_state SET week_start=? WHERE id=1")) {
            p.setDate(1,java.sql.Date.valueOf(week));p.executeUpdate();
        }
    }
    private Snapshot readSnapshot(Connection c,LocalDate week) throws SQLException {
        List<TOP> top=new ArrayList<>();Map<Long,Integer> rewards=new HashMap<>();
        try(PreparedStatement p=c.prepareStatement("SELECT player_id,wins FROM whis_weekly_score WHERE week_start=? AND wins>0 ORDER BY wins DESC,last_win ASC,player_id ASC LIMIT 100")) {
            p.setDate(1,java.sql.Date.valueOf(week));
            try(ResultSet r=p.executeQuery()) {
                while(r.next()) {
                    TOP row=TOP.builder().id_player(r.getInt(1)).build();
                    row.setInfo1(r.getInt(2)+" trận thắng tuần");row.setInfo2("Tuần từ "+week+" | Chốt thứ Hai 00:00");top.add(row);
                }
            }
        }
        try(PreparedStatement p=c.prepareStatement("SELECT player_id,rank_no FROM whis_weekly_reward WHERE reward_week=?")) {
            p.setDate(1,java.sql.Date.valueOf(week));
            try(ResultSet r=p.executeQuery()) { while(r.next())rewards.put(r.getLong(1),WhisWeeklyPolicy.bonusPercent(r.getInt(2))); }
        }
        return new Snapshot(week,List.copyOf(top),Map.copyOf(rewards));
    }
}
