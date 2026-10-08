package nro.models.services;

import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import nro.models.network.Message;
import nro.models.server.Manager;
import org.json.simple.JSONObject;

/** Pure JDBC/packet fixtures: no live server, database, player or broadcast. */
public final class LiveSyncExpNoticeTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final class Store {
        String value="7";long version=1;int writes;
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(o,m,a)-> {
            if(m.getName().equals("prepareStatement")) {
                String sql=(String)a[0];Object[] args=new Object[4];
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(p,n,b)-> {
                    if(n.getName().startsWith("set")){args[(Integer)b[0]]=b[1];return null;}
                    if(n.getName().equals("executeUpdate")){value=String.valueOf(args[1]);version++;writes++;return 1;}
                    if(n.getName().equals("executeQuery")){boolean[] first={true};return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(r,x,c)->switch(x.getName()) {
                        case "next" -> {boolean yes=first[0]&&value!=null;first[0]=false;yield yes;}
                        case "getString" -> value;case "getLong" -> version;default -> null;
                    });}return null;
                });
            }return null;
        });}
    }
    public static void main(String[] args)throws Exception {
        Store db=new Store();JSONObject p=new JSONObject();p.put("rate",12L);byte before=Manager.RATE_EXP_SERVER;
        var op=LiveSyncCatalogCommands.stage(db.connection(),"SET_EXP_RATE",p);
        check(db.writes==1&&db.value.equals("12"),"EXP not persisted in staging transaction");
        check(Manager.RATE_EXP_SERVER==before,"EXP published before commit");
        op.publish();check(Manager.RATE_EXP_SERVER==12,"Committed EXP not published");
        db.value="19";db.version++;
        var restore=LiveSyncCatalogCommands.class.getMethod("restore",Connection.class,String.class,JSONObject.class);
        var replay=restore.invoke(null,db.connection(),"SET_EXP_RATE",p);
        replay.getClass().getMethod("publish").invoke(replay);
        check(db.writes==1&&Manager.RATE_EXP_SERVER==19,"Receipt replay reverted latest EXP");
        Class<?> dao=Class.forName("nro.models.services.LiveSettingsDAO");
        check((Integer)dao.getMethod("loadExpRate",Connection.class,int.class).invoke(null,db.connection(),3)==19,"Restart ignores persisted EXP");
        db.value=null;check((Integer)dao.getMethod("loadExpRate",Connection.class,int.class).invoke(null,db.connection(),3)==3,"Missing setting ignores config fallback");
        Class<?> arithmetic=Class.forName("nro.models.player.ExpArithmetic");
        Method multiply=arithmetic.getMethod("multiply",long.class,long.class);
        check((Long)multiply.invoke(null,30_000_000L,100L)==3_000_000_000L,"EXP narrowed to int");
        check((Long)multiply.invoke(null,Long.MAX_VALUE,100L)==Long.MAX_VALUE,"EXP multiplication overflow");
        Message msg=(Message)ChatGlobalService.class.getMethod("systemNoticeMessage",String.class).invoke(null,"fixture notice");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(msg.getData()));
        check(msg.command==92&&!in.readUTF().isBlank(),"Notice missing world packet/name");
        check(in.readUTF().equals("|5|fixture notice"),"Notice text altered");in.readInt();
        check(in.readShort()>=0,"Invalid notice head");in.readShort();check(in.readShort()>=0,"Invalid body");in.readShort();check(in.readShort()>=0,"Invalid leg");in.readByte();check(in.available()==0,"Packet92 trailing fields");msg.cleanup();
        JSONObject notice=new JSONObject();notice.put("message","x".repeat(101));
        try{LiveSyncCatalogCommands.stage(db.connection(),"SEND_NOTICE",notice);throw new AssertionError("Oversize world notice accepted");}catch(IllegalArgumentException expected){}
        System.out.println("LiveSyncExpNoticeTest PASS");
    }
}
