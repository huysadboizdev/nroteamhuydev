package nro.models.data;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import nro.models.player_system.Template.Part;
import nro.models.player_system.Template.PartDetail;

/** The client uses array position as part ID; validate everything before replacing its data. */
public final class PartDataWriter {
    private PartDataWriter() { }

    public static byte[] encode(List<Part> input) throws IOException {
        List<Part> parts = new ArrayList<>(input);
        parts.sort(Comparator.comparingInt(p -> p.id));
        if (parts.isEmpty() || parts.size() > 32767) throw new IOException("Invalid part count");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeShort(parts.size());
            for (int id = 0; id < parts.size(); id++) {
                Part part = parts.get(id);
                int frames = switch (part.type) { case 0 -> 3; case 1 -> 17; case 2 -> 14; default -> -1; };
                if (part.id != id || frames < 0 || part.partDetails.size() != frames)
                    throw new IOException("Invalid part " + part.id + ": expected ID " + id
                            + ", type " + part.type + ", frames " + frames + ", got " + part.partDetails.size());
                out.writeByte(part.type);
                for (PartDetail frame : part.partDetails) {
                    if (frame.iconId < 0 || frame.iconId > 32767) throw new IOException("Invalid icon in part " + id);
                    out.writeShort(frame.iconId); out.writeByte(frame.dx); out.writeByte(frame.dy);
                }
            }
        }
        return bytes.toByteArray();
    }

    public static void write(List<Part> parts, Path destination) throws IOException {
        byte[] data = encode(parts);
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(), "part-", ".tmp");
        try {
            Files.write(temporary, data);
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
