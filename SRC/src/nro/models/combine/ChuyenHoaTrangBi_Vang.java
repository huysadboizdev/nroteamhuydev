package nro.models.combine;

import nro.models.player.Player;

public final class ChuyenHoaTrangBi_Vang {
    private ChuyenHoaTrangBi_Vang() { }
    public static void chuyenHoaTrangBiVang(Player player) {
        ChuyenHoaTrangBiRules.preview(player, false);
    }
    public static void thucHienChuyenHoa(Player player) {
        ChuyenHoaTrangBiRules.convert(player, false);
    }
}
