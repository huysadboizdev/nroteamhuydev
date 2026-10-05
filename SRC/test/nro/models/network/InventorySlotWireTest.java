package nro.models.network;
import java.io.*; import java.lang.reflect.*; import java.util.*;
import nro.models.interfaces.ISession;
public final class InventorySlotWireTest {
 static Message packet(int cmd,byte[] data){return new Message(cmd){@Override public byte[] getData(){return data;}};}
 public static void main(String[] args) throws Exception {
  Class<?> wire=Class.forName("nro.models.network.InventoryWire");
  var read=wire.getMethod("readSlots",DataInputStream.class,int.class);
  int[] slots=(int[])read.invoke(null,new DataInputStream(new ByteArrayInputStream(new byte[]{3,127,(byte)128,(byte)199})),200);
  if(!Arrays.equals(slots,new int[]{127,128,199}))throw new AssertionError("unsigned slots");
  for(byte[] bad:new byte[][]{{2,1,1},{1,(byte)200},{2,1},{(byte)201}}){try{read.invoke(null,new DataInputStream(new ByteArrayInputStream(bad)),200);throw new AssertionError("invalid selection accepted");}catch(InvocationTargetException expected){if(!(expected.getCause() instanceof IOException))throw expected;}}
  for(boolean encrypted:new boolean[]{false,true})for(boolean extended:new boolean[]{false,true})for(int size:new int[]{0,1,65535,65536,100000}) {
   byte[] key={3,17,91};ISession s=(ISession)Proxy.newProxyInstance(ISession.class.getClassLoader(),new Class[]{ISession.class},(o,m,a)->switch(m.getName()){case "getKey"->key;case "sentKey"->encrypted;case "supportsInventory200"->extended;default->null;});
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();MessageSendCollect send=new MessageSendCollect();byte[] payload=new byte[size];Arrays.fill(payload,(byte)73);
   try{send.doSendMessage(s,new DataOutputStream(bytes),packet(-36,size==0?null:payload));if(!extended&&size>65535)throw new AssertionError("legacy overflow accepted");}catch(IllegalArgumentException e){if(extended||size<=65535||bytes.size()!=0)throw e;continue;}
   send.doSendMessage(s,new DataOutputStream(bytes),packet(42,new byte[]{19}));
   byte[] raw=bytes.toByteArray();int cursor=0, ki=0;
   int cmd=raw[cursor++]&255;if(encrypted)cmd^=key[ki++%3]&255;if(cmd!=220)throw new AssertionError("command");
   int count=extended?3:2, length=0;
   for(int i=0;i<count;i++){int b=raw[cursor++]&255;if(extended)b=(b+128)&255;if(encrypted)b^=key[ki++%3]&255; length|=b << (extended?8*i:8*(1-i));}
   if(length!=size)throw new AssertionError("length "+size+" -> "+length);
   for(int i=0;i<size;i++){int b=raw[cursor++]&255;if(encrypted)b^=key[ki++%3]&255;if(b!=73)throw new AssertionError("payload");}
   cmd=raw[cursor++]&255;if(encrypted)cmd^=key[ki++%3]&255;if(cmd!=42)throw new AssertionError("next command");
   for(int expected:new int[]{0,1,19}){int b=raw[cursor++]&255;if(encrypted)b^=key[ki++%3]&255;if(b!=expected)throw new AssertionError("next packet alignment");}
   if(cursor!=raw.length)throw new AssertionError("trailing data");
  }
  System.out.println("PASS slots validation and plaintext/XOR frames, null, 65535/65536/100000, next packet");
 }
}

