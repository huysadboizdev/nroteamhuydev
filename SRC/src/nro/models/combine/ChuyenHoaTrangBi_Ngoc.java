package nro.models.combine;

import nro.models.player.Player;

public final class ChuyenHoaTrangBi_Ngoc {
    private ChuyenHoaTrangBi_Ngoc() { }
    public static void chuyenHoaTrangBiNgoc(Player player) {
        ChuyenHoaTrangBiRules.preview(player, true);
    }
    public static void thucHienChuyenHoa(Player player) {
        ChuyenHoaTrangBiRules.convert(player, true);
    }
}
