package nro.models.services;

import java.util.concurrent.ThreadLocalRandom;
import nro.models.item.Item;
import nro.models.player.Player;

/** Exchange the evolved Mabu forms; this is unrelated to the companion NewPet. */
public final class AdvancedPetExchange {
    public static final int ITEM_ID = 1890;
    private AdvancedPetExchange() { }

    public static boolean hasEligiblePet(Player player) {
        return player != null && player.pet != null
                && player.pet.typePet >= 2 && player.pet.typePet <= 4;
    }

    public static String exchange(Player player, Item item) {
        synchronized (player) {
            if (CommesonChallengeService.hasPendingId(player.id)
                    || RubyGiftService.hasPending(player)) return "Giao dịch đang chờ xử lý, hãy thử lại sau.";
            if (!hasEligiblePet(player)) return "Cần có đệ Uub, Berus nhí hoặc Jiren để đổi đệ tử 2.";
            if (item == null || !item.isNotNullItem() || item.template.id != ITEM_ID
                    || item.quantity <= 0 || !player.inventory.itemsBag.contains(item)) {
                return "Vật phẩm đổi đệ tử 2 không hợp lệ.";
            }
            for (Item equipped : player.pet.inventory.itemsBody) {
                if (equipped != null && equipped.isNotNullItem()) {
                    return "Hãy tháo toàn bộ trang bị của đệ tử trước khi đổi đệ tử 2.";
                }
            }
            int next = 2 + ((player.pet.typePet - 2 + 1 + ThreadLocalRandom.current().nextInt(2)) % 3);
            switch (next) {
                case 2: PetService.gI().changeUubPet(player); break;
                case 3: PetService.gI().changeKidBeerPet(player); break;
                case 4: PetService.gI().changeJirenPet(player); break;
                default: throw new IllegalStateException("Invalid advanced pet");
            }
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            return "Đã đổi sang đệ tử mới: " + player.pet.name + ".";
        }
    }
}
