package nro.models.data;

import java.io.IOException;
import java.nio.file.*;

/** Tile layout: unsigned width, unsigned height, then width * height unsigned tiles. */
public final class TileMapData {
    private TileMapData() { }

    public static int[][] read(int mapId) throws IOException {
        byte[] data = Files.readAllBytes(Path.of("data/map/tile_map_data", String.valueOf(mapId)));
        if (data.length < 2) throw new IOException("Missing tile header for map " + mapId);
        int width = data[0] & 255, height = data[1] & 255;
        // Some shipped tiles have a single legacy footer byte after the grid.
        int expected = 2 + width * height;
        if (width == 0 || height == 0 || data.length < expected || data.length > expected + 1)
            throw new IOException("Invalid tile layout for map " + mapId);
        int[][] tiles = new int[height][width];
        for (int y=0; y<height; y++) for (int x=0; x<width; x++) tiles[y][x] = data[2+y*width+x] & 255;
        return tiles;
    }
}
