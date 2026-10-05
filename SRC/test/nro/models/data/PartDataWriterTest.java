package nro.models.data;

import java.nio.file.*;
import java.util.*;
import java.io.*;
import nro.models.player_system.Template.Part;
import nro.models.player_system.Template.PartDetail;

public class PartDataWriterTest {
    private static Part part(int id,int type,int count) {
        Part p=new Part();p.id=id;p.type=type;
        for(int i=0;i<count;i++)p.partDetails.add(new PartDetail(2955,(byte)0,(byte)0));
        return p;
    }
    private static void rejects(List<Part> parts) throws Exception {
        Path dir=Files.createTempDirectory("part-writer-test"),file=dir.resolve("part");
        byte[] previous={1,2,3};Files.write(file,previous);
        try {
            try {PartDataWriter.write(parts,file);throw new AssertionError("Accepted corrupt parts");}
            catch(IOException expected) { }
            if(!Arrays.equals(Files.readAllBytes(file),previous))throw new AssertionError("Replaced last good file");
        } finally {Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }
    public static void main(String[] args) throws Exception {
        List<Part> ordered=List.of(part(0,0,3),part(1,1,17),part(2,2,14));
        byte[] data=PartDataWriter.encode(ordered);
        if(!Arrays.equals(data,PartDataWriter.encode(List.of(ordered.get(2),ordered.get(0),ordered.get(1)))))
            throw new AssertionError("Row order changed client IDs");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(data))) {
            if(in.readUnsignedShort()!=3)throw new AssertionError();
            for(int type=0;type<3;type++) {
                if(in.readUnsignedByte()!=type)throw new AssertionError();
                for(int i=0;i<new int[]{3,17,14}[type];i++) {
                    if(in.readUnsignedShort()!=2955||in.readByte()!=0||in.readByte()!=0)throw new AssertionError();
                }
            }
            if(in.available()!=0)throw new AssertionError("Trailing bytes");
        }
        rejects(List.of(part(0,0,2)));
        rejects(List.of(part(0,0,3),part(0,0,3)));
        rejects(List.of(part(0,0,3),part(2,1,17)));
        rejects(List.of(part(0,3,3)));
        System.out.println("PART_WRITER=PASS ordered,frames,duplicate,gap,type,atomic-preservation");
    }
}
