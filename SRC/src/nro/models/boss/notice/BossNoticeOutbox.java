package nro.models.boss.notice;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import nro.models.database.BossNoticeDAO;

/** Durable immutable snapshots; replay is safe even if SQL committed before delete failed. */
public final class BossNoticeOutbox {
    private final Path dir;private final Gson gson=new Gson();
    public BossNoticeOutbox(Path dir){this.dir=dir;}
    public synchronized void stage(BossNoticeRow row)throws IOException{
        Files.createDirectories(dir);Path target=dir.resolve(row.spawnId()+"-"+row.version()+".json");
        if(Files.exists(target))return;
        Path temp=Files.createTempFile(dir,"stage-",".tmp");
        try{
            byte[] bytes=gson.toJson(row).getBytes(StandardCharsets.UTF_8);
            try(FileChannel c=FileChannel.open(temp,StandardOpenOption.WRITE)){ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())c.write(b);c.force(true);}
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException noAtomic){Files.move(temp,target);}
        }finally{Files.deleteIfExists(temp);}
    }
    public int drain(BossNoticeDAO dao,int limit)throws Exception{
        if(!Files.isDirectory(dir))return 0;List<Path> files;
        try(var list=Files.list(dir)){files=list.filter(p->p.getFileName().toString().matches("[0-9a-f-]{36}-[0-9]+\\.json")).sorted().limit(limit).toList();}
        int count=0;for(Path p:files){
            // The bundled Gson predates Java records; construct via the canonical constructor.
            var r=new JsonParser().parse(Files.readString(p)).getAsJsonObject();
            BossNoticeRow row=new BossNoticeRow(r.get("spawnId").getAsString(),r.get("serverRun").getAsString(),r.get("bossId").getAsLong(),r.get("form").getAsInt(),r.get("bossName").getAsString(),r.get("mapName").getAsString(),r.get("appearedAt").getAsLong(),r.get("defeatedAt").getAsLong(),r.get("killerName").getAsString(),r.get("status").getAsString(),r.get("nextSpawnAt").getAsLong(),r.get("etaKind").getAsString(),r.get("version").getAsLong(),r.get("updatedAt").getAsLong());
            dao.save(row);Files.deleteIfExists(p);count++;
        }return count;
    }
}
