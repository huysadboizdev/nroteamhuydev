package nro.models.managers;
import nro.models.data.LocalManager;
import nro.models.player_system.GiftCode;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import java.util.ArrayList;
import nro.models.services.InventoryService;

public class GiftCodeManager {

    public String name;
    public volatile ArrayList<GiftCode> listGiftCode = new ArrayList<>();

    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) {
            instance = new GiftCodeManager();
        }
        return instance;
    }

    /** Preview only. Redemption must go through GiftCodeService's transaction. */
    public synchronized GiftCode checkUseGiftCode(Player player,String raw) {
        String code=nro.models.player.GiftCode.canonical(raw);
        for(GiftCode gift:listGiftCode)if(nro.models.player.GiftCode.canonical(gift.code).equals(code))return gift;
        return null;
    }
    /** Legacy callers may not write a cached quota back over authoritative SQL. */
    @Deprecated public synchronized void updateGiftCode(GiftCode gift) {}
    public synchronized void publishClaim(String code,int catalogId,int remaining) {
        // An older uncertain commit may resolve after a newer claim/reload. Never refill its cache.
        for(GiftCode gift:listGiftCode)if(gift.id==catalogId&&nro.models.player.GiftCode.canonical(gift.code).equals(code)
                &&gift.countLeft>=0&&remaining>=0)gift.countLeft=Math.min(gift.countLeft,remaining);
    }

    public void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (GiftCode giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng còn lại: ").append(giftCode.countLeft).append("\b")
                    .append("Ngày tạo: ")
                    .append(giftCode.datecreate).append(", Ngày hết hạn: ").append(giftCode.dateexpired)
                    .append("\n");
        }
        if(sb.length()>0)sb.deleteCharAt(sb.length() - 1);
        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }

}
