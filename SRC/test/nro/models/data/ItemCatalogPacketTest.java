package nro.models.data;
import java.util.*;
import java.io.*;
import java.net.Socket;
import nro.models.network.*;
import nro.models.server.Manager;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;
public class ItemCatalogPacketTest {
 static class Capture extends MySession {
  List<byte[]> packets;
  Capture(){super((Socket)null);}
  @Override public void doSendMessage(Message m){byte[] b=m.getData();check(b.length<=65535,"packet must fit frame");packets.add(b.clone());}
 }
 static void fixture(int count,boolean heavy){
  Manager.ITEM_TEMPLATES.clear();
  for(int i=0;i<count;i++){var t=new Template.ItemTemplate();t.id=(short)i;t.type=(byte)(i%5);t.gender=3;t.name="Item "+i;t.description=(heavy&&i>=750?"x".repeat(50):"short");t.iconID=(short)i;t.part=(short)-1;Manager.ITEM_TEMPLATES.add(t);}
 }
 static Capture capture()throws Exception{Capture c=blank(Capture.class);c.packets=new ArrayList<>();return c;}
 static void verify(Capture c,int count)throws Exception{
  int next=0,groups=0;
  for(byte[] b:c.packets){var in=new DataInputStream(new ByteArrayInputStream(b));check(in.readByte()==8,"item envelope");in.readByte();int type=in.readUnsignedByte();if(type!=1&&type!=2)continue;groups++;
   int start=type==1?0:in.readUnsignedShort();int end=in.readUnsignedShort();check(start==next,"contiguous cache chunks");
   for(int i=start;i<end;i++){check(in.readByte()==i%5,"type "+i);in.readByte();check(in.readUTF().equals("Item "+i),"id/name "+i);in.readUTF();in.readByte();in.readInt();check(in.readShort()==i,"icon "+i);in.readShort();in.readBoolean();}check(in.available()==0,"payload fully read");next=end;
  }
  check(groups==2&&next==count,"complete two-group catalog");
 }
 public static void main(String[] args)throws Exception{
  fixture(2040,true);Capture c=capture();ItemData.updateItem(c);verify(c,2040);
  for(int count:new int[]{0,1,50,750,1000}){fixture(count,false);c=capture();ItemData.updateItem(c);verify(c,count);}
  fixture(3000,true);c=capture();boolean rejected=false;try{ItemData.updateItem(c);}catch(IllegalArgumentException e){rejected=true;}check(rejected&&c.packets.isEmpty(),"reject before sending incomplete update");
  System.out.println("PASS actual ItemData: oversized tail split, all IDs/cache groups, small catalogs, over-capacity rejection");
 }
}
