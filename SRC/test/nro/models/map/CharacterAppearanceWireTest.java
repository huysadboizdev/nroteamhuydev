package nro.models.map;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossesData;
import nro.models.boss.Frieza.Fide;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.PinkFixture;
import nro.models.services.Service;
import nro.models.Bot.BotAttackplayer;

/** Mirrors the -5 client reader through its mandatory trailing badge byte. */
public final class CharacterAppearanceWireTest {
    public static class Viewer extends Player {
        byte[] appearance;
        @Override public void sendMessage(Message message) {
            if (message.command == -5) appearance = message.getData();
        }
    }
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static void copyPlayer(Player from, Player to) throws Exception {
        for (Field field : Player.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            field.set(to, field.get(from));
        }
    }
    static void readAppearance(byte[] packet, Player expected) throws Exception {
        check(packet != null, "appearance must be sent");
        var in = new DataInputStream(new ByteArrayInputStream(packet));
        check(in.readInt() == (int) expected.id, "character identity");
        in.readInt(); in.readByte(); in.readBoolean(); in.readByte(); in.readByte(); in.readByte();
        check(in.readShort() == (expected instanceof BotAttackplayer ? expected.head : expected.getHead()), "head");
        check(in.readUTF().equals(expected.name), "name");
        check(in.readInt() == expected.nPoint.hp && in.readInt() == expected.nPoint.hpMax, "HP");
        int body = expected instanceof BotAttackplayer ? expected.body : expected.getBody();
        int leg = expected instanceof BotAttackplayer ? expected.leg : expected.getLeg();
        check(in.readShort() == body && in.readShort() == leg, "body and leg");
        in.readByte(); in.readByte(); in.readShort(); in.readShort(); in.readShort(); in.readShort();
        int effects = in.readUnsignedByte();
        for (int i = 0; i < effects; i++) {
            in.readByte(); in.readInt(); in.readInt(); in.readShort();
        }
        in.readByte(); in.readByte(); in.readShort(); in.readByte(); in.readByte();
        in.readShort(); in.readByte(); in.readShort();
        check(in.available() == 1, "client requires exactly one badge byte after hat; remaining=" + in.available());
        check(in.readUnsignedByte() == (expected.isNewMember ? 1 : 0), "badge value");
        check(in.available() == 0, "exact packet consumption");
    }
    public static void main(String[] args) {
        try {
            PinkFixture.install();
            Viewer viewer = new Viewer();
            Fide fide = PinkFixture.blank(Fide.class);
            copyPlayer(new Player(), fide);
            fide.id = -28; fide.isBoss = true; fide.isPlayer = false;
            fide.name = "Fide"; fide.nPoint.hp = 50000000; fide.nPoint.hpMax = 50000000;
            Field data = Boss.class.getDeclaredField("data"); data.setAccessible(true);
            data.set(fide, new BossData[]{BossesData.FIDE_DAI_CA_1, BossesData.FIDE_DAI_CA_2, BossesData.FIDE_DAI_CA_3});
            Method info = Zone.class.getDeclaredMethod("infoPlayer", Player.class, Player.class); info.setAccessible(true);
            Zone zone = PinkFixture.zone();
            for (int form = 0; form < 3; form++) {
                fide.currentLevel = form;
                for (boolean badge : new boolean[]{false, true}) {
                    fide.isNewMember = badge; viewer.appearance = null;
                    info.invoke(zone, viewer, fide);
                    readAppearance(viewer.appearance, fide);
                }
            }
            BotAttackplayer bot = PinkFixture.blank(BotAttackplayer.class);
            copyPlayer(new Player(), bot);
            bot.id = -12345; bot.name = "bot fixture"; bot.isBot = true; bot.isPlayer = false;
            bot.head = 183; bot.body = 184; bot.leg = 185; bot.nPoint.hp = 100; bot.nPoint.hpMax = 100;
            viewer.session = PinkFixture.blank(nro.models.network.MySession.class);
            for (boolean badge : new boolean[]{false, true}) {
                bot.isNewMember = badge; viewer.appearance = null;
                Service.gI().sendInfoCharMoiToMe(viewer, bot);
                readAppearance(viewer.appearance, bot);
            }
            System.out.println("CHARACTER_APPEARANCE_WIRE PASS: 3 Fide forms and bot, both badge states (8 cases)");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace(); System.exit(1);
        }
    }
}
