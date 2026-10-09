package nro.models.services;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import nro.models.network.Message;
import nro.models.player_system.Template.ItemTemplate;

public final class VillageRankingTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static VillageRankingService.Row row(int id, String power, String task) {
        return VillageRankingService.parseRow(id,"p"+id,(short)64,(byte)0,power,task,"[]",ignored -> null);
    }
    public static void main(String[] args) throws Exception {
        check(VillageRankingService.formatPower(59400007888L).equals("59,4 tỷ"),"compact billion");
        check(VillageRankingService.formatPower(850000000L).equals("850 tr"),"compact million");
        check(VillageRankingService.formatPower(120000L).equals("120 nghìn"),"compact thousand");
        check(VillageRankingService.formatPower(999L).equals("999"),"small power");
        check(VillageRankingService.formatPower(999999999L).equals("999,9 tr"),"truncate boundary safely");
        check(VillageRankingService.formatPower(Long.MAX_VALUE).equals("9223372036,8 tỷ"),"long without overflow");
        check(VillageRankingService.formatPower(0).equals("0"),"zero");
        var first=row(4,"[0,10000000000]","[12,2,9,0]");
        var second=row(2,"[0,59400007888]","[12,3,0,0]");
        var third=row(1,"[0,59400007888]","[12,3,0,0]");
        var higher=row(8,"[0,50]","[13,0,0,0]");
        var ordered=VillageRankingService.rank(List.of(first,second,third,higher),VillageRankingService.Kind.TASK);
        check(ordered.get(0).id()==8 && ordered.get(1).id()==1 && ordered.get(2).id()==2,"descending task tuple stable id");
        check(VillageRankingService.rank(List.of(first,second,third),VillageRankingService.Kind.POWER).get(0).id()==1,"descending power stable id");
        check(VillageRankingService.taskInfo(first).equals("12"),"task number only");
        check(row(1,"garbage","[0,0,0]")==null,"malformed JSON skipped");
        check(row(1,"[0,1]","[1,-1,0]")==null,"negative task invalid");
        check(row(1,"[0,1.5]","[1,0,0]")==null,"fractional power invalid");
        ItemTemplate shirt=new ItemTemplate(); shirt.part=99;
        ItemTemplate costume=new ItemTemplate(); costume.head=201; costume.body=202; costume.leg=-1;
        var appearance=VillageRankingService.parseRow(1,"x",(short)64,(byte)1,"[0,1]","[1,0,0]","[\"[10,1,[],0]\",\"[-1,0,[],0]\",null,null,null,\"[20,1,[],0]\"]",id->id==10?shirt:id==20?costume:null);
        check(appearance.head()==201 && appearance.body()==202 && appearance.leg()==60,"costume overrides each part and native fallback");
        var expired=VillageRankingService.parseRow(1,"x",(short)64,(byte)0,"[0,1]","[1,0,0]","[null,null,null,null,null,\"[20,1,[[93,0]],0]\"]",id->costume);
        check(expired.head()==64 && expired.body()==57 && expired.leg()==58,"expired costume falls back without restoring inventory");
        for(int version:new int[]{214,215}) {
            Message packet=new Message(-96);
            VillageRankingService.writePacket(packet,ordered,VillageRankingService.Kind.TASK,version);
            var in=new DataInputStream(new ByteArrayInputStream(packet.getData()));
            check(in.readUnsignedByte()==0 && in.readUTF().equals("Top nhiệm vụ") && in.readUnsignedByte()==4,"packet header and exact count");
            for(int i=0;i<4;i++){check(in.readInt()==i+1,"ordinal");check(in.readInt()==ordered.get(i).id(),"id");in.readShort();if(version>214)check(in.readShort()==-1,"extra head");in.readShort();in.readShort();in.readUTF();check(in.readUTF().equals(VillageRankingService.taskInfo(ordered.get(i))),"compact task wire");check(in.readUTF().isEmpty(),"no verbose secondary info");}
            check(in.available()==0,"exact row layout");packet.cleanup();
        }
        var many=new ArrayList<VillageRankingService.Row>(); for(int i=1;i<=150;i++)many.add(row(i,"[0,"+i+"]","[1,0,0]"));
        check(VillageRankingService.rank(many,VillageRankingService.Kind.POWER).size()==100,"top100 cap");
        Message powerPacket=new Message(-96);
        VillageRankingService.writePacket(powerPacket,List.of(second,third),VillageRankingService.Kind.POWER,214);
        var powerIn=new DataInputStream(new ByteArrayInputStream(powerPacket.getData()));
        powerIn.readByte();check(powerIn.readUTF().equals("Top sức mạnh") && powerIn.readUnsignedByte()==2,"power category title");
        for(int i=0;i<2;i++){powerIn.readInt();powerIn.readInt();powerIn.readShort();powerIn.readShort();powerIn.readShort();powerIn.readUTF();check(powerIn.readUTF().equals("59,4 tỷ") && powerIn.readUTF().isEmpty(),"compact power wire");}
        check(powerIn.available()==0,"power exact layout");powerPacket.cleanup();
        long[] now={0}; int[] calls={0}; boolean[] broken={false};
        var service=new VillageRankingService(kind->{calls[0]++;if(broken[0])throw new SQLException("offline");return many;},()->now[0]);
        check(service.rows(VillageRankingService.Kind.POWER).size()==100,"cache bounded");
        service.rows(VillageRankingService.Kind.POWER);check(calls[0]==1,"fresh cache reused");
        now[0]=60000;service.rows(VillageRankingService.Kind.POWER);check(calls[0]==2,"cache expires at60seconds");
        now[0]=120000;broken[0]=true;try{service.rows(VillageRankingService.Kind.POWER);throw new AssertionError("expired cache returned on DB failure");}catch(SQLException expected){}
        broken[0]=false;service.rows(VillageRankingService.Kind.POWER);check(calls[0]==4,"failed refresh can recover");
        service.rows(VillageRankingService.Kind.TASK);check(calls[0]==5,"categories have independent cache");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(8);
        var concurrentCalls=new java.util.concurrent.atomic.AtomicInteger();
        var concurrentService=new VillageRankingService(kind->{concurrentCalls.incrementAndGet();return many;},()->0L);
        try {
            var jobs=new ArrayList<java.util.concurrent.Callable<List<VillageRankingService.Row>>>();
            for(int i=0;i<16;i++)jobs.add(()->concurrentService.rows(VillageRankingService.Kind.POWER));
            for(var result:pool.invokeAll(jobs))check(result.get().size()==100,"concurrent bounded result");
            check(concurrentCalls.get()==1,"concurrent requests share single refresh");
        } finally { pool.shutdownNow(); }
        System.out.println("PASS VillageRanking: formatting, progress ordering, malformed saves, appearance, packets, top100/cache expiry/failure");
    }
}
