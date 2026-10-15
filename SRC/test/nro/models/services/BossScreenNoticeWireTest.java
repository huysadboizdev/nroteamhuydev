package nro.models.services;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import nro.models.network.Message;
import nro.models.server.ServerNotify;

/** Task 2 wire test: additive screen event keeps ticker 93 unchanged, fail-open on unknown/legacy. */
public final class BossScreenNoticeWireTest {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        String ticker = "BOSS Fide v\u1eeba xu\u1ea5t hi\u1ec7n t\u1ea1i N\u00fai";
        int semanticId = -28;
        int form = 0;
        int mapId = 5;
        int mapType = 0;

        String key1 = BossNoticeService.screenEventKey(semanticId, form, mapId, mapType);
        String key2 = BossNoticeService.screenEventKey(semanticId, form, mapId, mapType);
        check(key1 != null && !key1.isEmpty(), "eventKey present");
        check(key1.equals(key2), "eventKey stable");
        check(!key1.equals(BossNoticeService.screenEventKey(9999, form, mapId, mapType)),
                "eventKey distinct per identity");

        check(ServerNotify.SCREEN_COMMAND != 93, "screen event uses additive command, keeps 93 for ticker");
        check(ServerNotify.SCREEN_COMMAND == 115, "screen event uses reserved command 115");

        Message tickerMsg = new Message(93);
        tickerMsg.writer().writeUTF(ticker);
        DataInputStream tickerIn = new DataInputStream(new ByteArrayInputStream(tickerMsg.getData()));
        check(tickerIn.readUTF().equals(ticker) && tickerIn.available() == 0,
                "command 93 ticker unchanged with no trailing metadata");

        Message screen = ServerNotify.buildScreenMessage(key1, semanticId, form, mapId, mapType, ticker, true);
        check(screen.command == (byte) ServerNotify.SCREEN_COMMAND, "screen packet command");
        ServerNotify.BossScreenEvent decoded = ServerNotify.decodeScreenMessage(screen.getData());
        check(decoded != null, "screen event decodes");
        check(decoded.eventKey().equals(key1), "eventKey round-trip");
        check(decoded.semanticBossId() == semanticId, "semanticBossId round-trip");
        check(decoded.form() == form, "form round-trip");
        check(decoded.mapId() == mapId, "mapId round-trip");
        check(decoded.mapType() == mapType, "mapType round-trip");
        check(decoded.ticker().equals(ticker), "ticker round-trip without mutation");
        check(decoded.hiddenFromPanel(), "hidden flag round-trip");
        DataInputStream wire = new DataInputStream(new ByteArrayInputStream(screen.getData()));
        check(wire.readUnsignedByte() == ServerNotify.SCREEN_SCHEMA, "schema on wire");
        check(wire.readUnsignedByte() == 2, "screen wire version 2");
        check(wire.readUTF().equals(key1), "wire event key order");
        check(wire.readInt() == semanticId && wire.readInt() == form && wire.readInt() == mapId
                && wire.readInt() == mapType, "wire identity order");
        check(wire.readUTF().equals(ticker), "wire ticker order");
        check(wire.readBoolean() && wire.available() == 0, "hidden flag follows ticker without trailing data");

        Message visible = ServerNotify.buildScreenMessage(key1, semanticId, form, mapId, mapType, ticker, false);
        check(!ServerNotify.decodeScreenMessage(visible.getData()).hiddenFromPanel(),
                "visible flag round-trip");

        Message unknown = ServerNotify.buildScreenMessageWithVersion(99, 99, key1, semanticId, form, mapId,
                mapType, ticker, true);
        check(ServerNotify.decodeScreenMessage(unknown.getData()) == null,
                "unknown schema/version ignored fail-open");
        Message oldVersion = ServerNotify.buildScreenMessageWithVersion(1, 1, key1, semanticId, form, mapId,
                mapType, ticker, true);
        check(ServerNotify.decodeScreenMessage(oldVersion.getData()) == null,
                "version 1 without an authoritative flag is rejected");
        byte[] truncated = java.util.Arrays.copyOf(screen.getData(), screen.getData().length - 1);
        check(ServerNotify.decodeScreenMessage(truncated) == null,
                "missing hidden flag is rejected");

        check(ServerNotify.decodeScreenMessage(tickerMsg.getData()) == null,
                "old text-only packet decodes safely fail-open");
        check(ServerNotify.decodeScreenMessage(new byte[0]) == null,
                "truncated packet decodes safely fail-open");

        System.out.println("PASS BossScreenNoticeWire: ticker intact, structured event stable, fail-open");
    }
}
