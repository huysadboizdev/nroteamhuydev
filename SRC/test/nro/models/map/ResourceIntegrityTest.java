package nro.models.map;

import java.lang.reflect.*;
import java.nio.file.*;
import nro.models.map.service.MapService;

/** Reads real tile files through both production readers, without starting a server. */
public class ResourceIntegrityTest {
    public static void main(String[] args) throws Exception {
        if (nro.models.data.DataGame.vsData != 10 || nro.models.data.DataGame.vsMap != 4
                || nro.models.data.DataGame.vsItem != 10 || nro.models.data.DataGame.vsRes != 2)
            throw new AssertionError("Client cache versions must include changed item templates");
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Object allocator = field.get(null);
        Method allocate = unsafe.getMethod("allocateInstance", Class.class);
        Map hidden = (Map) allocate.invoke(allocator, Map.class);
        hidden.npcs = new java.util.ArrayList<>();
        hidden.initNpc(new byte[]{108},new short[]{100},new short[]{100});
        if (!hidden.npcs.isEmpty()) throw new AssertionError("Heart spawned without real assets");
        int checked = 0;
        try (var files = Files.list(Path.of("data/map/tile_map_data"))) {
            for (Path path : files.toList()) {
                if (!path.getFileName().toString().matches("[0-9]+")) continue;
                int id = Integer.parseInt(path.getFileName().toString());
                byte[] data = Files.readAllBytes(path);
                int width = data[0] & 255, height = data[1] & 255;
                if (data.length < 2 + width * height || data.length > 3 + width * height)
                    throw new AssertionError("File layout " + id);
                Map map = (Map) allocate.invoke(allocator, Map.class);
                map.readTileMap(id);
                if (map.tmw != width || map.tmh != height || map.maps == null || map.types == null
                        || map.maps.length != width * height || map.types.length != width * height)
                    throw new AssertionError("Map reader " + id + " expected " + width + "x" + height
                            + " got " + map.tmw + "x" + map.tmh);
                int[][] tiles = MapService.gI().readTileMap(id);
                if (tiles == null || tiles.length != height || tiles[0].length != width)
                    throw new AssertionError("MapService reader " + id);
                for (int i=0; i<width*height; i++) {
                    if (map.maps[i] != (data[i+2]&255) || tiles[i/width][i%width] != (data[i+2]&255))
                        throw new AssertionError("Tile mismatch " + id + " at " + i);
                }
                checked++;
            }
        }
        System.out.println("TILE_READERS=PASS files=" + checked);
    }
}
