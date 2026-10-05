package nro.models.network;
import java.io.*;
public final class ItemOptionWireTest {
    public static void main(String[] args) throws Exception {
        Class<?> codec=Class.forName("nro.models.network.ItemOptionWire");
        var write=codec.getMethod("write",DataOutputStream.class,boolean.class,int.class);
        for(boolean extended:new boolean[]{false,true}) {
            for(int param:new int[]{0,1,32767,32768,65535,65536,100000,1000000,10000000,99999999,Integer.MAX_VALUE}) {
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                DataOutputStream out=new DataOutputStream(bytes);
                out.writeByte(31);write.invoke(null,out,extended,param);out.writeByte(73);out.writeInt(123456789);
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
                if(in.readUnsignedByte()!=31)throw new AssertionError("Option ID");
                int actual=extended?in.readInt():in.readUnsignedShort();
                int expected=extended?param:Math.min(param,65535);
                if(actual!=expected)throw new AssertionError(param+" -> "+actual);
                if(in.readUnsignedByte()!=73 || in.readInt()!=123456789 || in.available()!=0)throw new AssertionError("Packet misaligned");
            }
        }
        for (boolean extended : new boolean[]{false,true}) {
            for(int quantity:new int[]{1,32767,32768,100000,10000000,99999999}) {
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                DataOutputStream out=new DataOutputStream(bytes);
                codec.getMethod("writePickupQuantity",DataOutputStream.class,boolean.class,int.class)
                        .invoke(null,out,extended,quantity);
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
                if(in.readShort()!=(quantity>32767?9999:quantity))throw new AssertionError("Legacy pickup count");
                if(extended && in.readInt()!=quantity)throw new AssertionError("Full pickup count");
                if(in.available()!=0)throw new AssertionError("Pickup packet length");
            }
        }
        System.out.println("PASS: option int32/legacy boundaries and packet alignment");
    }
}
