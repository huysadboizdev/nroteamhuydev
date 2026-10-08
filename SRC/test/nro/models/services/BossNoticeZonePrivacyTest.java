package nro.models.services;
import java.io.*;
import java.lang.reflect.*;
import nro.models.boss.BossID;
import nro.models.boss.notice.*;
import nro.models.network.Message;
import static nro.models.services.PinkFixture.*;
public final class BossNoticeZonePrivacyTest {
 public static void main(String[] args)throws Exception {
  Method writer;
  try{writer=BossNoticeService.class.getMethod("writeCategoryMetadata",Message.class,BossNoticeRow.class);}
  catch(NoSuchMethodException missing){throw new AssertionError("BOSS2 must retain layout but mask zone",missing);}
  for(int category:new int[]{1,2}){
   var row=new BossNoticeRow("id","run",-927,0,"Mabu","Map",1,0,"","ALIVE",0,"",1,1,category,-927,category==1,9,2);
   Message m=new Message(-58);writer.invoke(null,m,row);
   DataInputStream in=new DataInputStream(new ByteArrayInputStream(m.getData()));
   check(in.readUnsignedByte()==category&&in.readInt()==-927&&in.readBoolean()==(category==1)&&in.readShort()==-1&&in.available()==0,"BOSS2 zone hidden without changing layout");
   check(row.zoneId()==9,"internal SQL snapshot keeps zone for diagnosis");m.cleanup();
  }
  check(BossNoticeCatalog.category(BossID.class.getField("FAT_MABU_TOURNAMENT").getInt(null),0)==2,"Mabu is general boss");
  System.out.println("PASS BOSS2 hides zone for both categories; internal snapshot retained; Mabu general category");
 }
}
