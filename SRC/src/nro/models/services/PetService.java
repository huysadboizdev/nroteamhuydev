package nro.models.services;

import nro.models.consts.ConstPlayer;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.PetSkillRules;
import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    // Legacy limitPower arguments are intentionally ignored: new pets must reopen every tier.
    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createPet(player, false, false, false, false, (byte) gender);
    }

    public void createNormalPet(Player player, byte... limitPower) {
        createPet(player, false, false, false, false);
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        createPet(player, true, false, false, false, (byte) gender);
    }

    public void createMabuPet(Player player, byte... limitPower) {
        createPet(player, true, false, false, false);
    }

    public void createUubPet(Player player, byte... limitPower) {
        createPet(player, false, true, false, false, (byte) player.gender);
    }

    public void createJirenPet(Player player, byte... limitPower) {
        createPet(player, false, false, false, true, (byte) player.gender);
    }

    public void createKidBeerPet(Player player, byte... limitPower) {
        createPet(player, false, false, true, false, (byte) player.gender);
    }

    private void createPet(Player player, boolean isMabu, boolean isUub, boolean isKidBeer,
            boolean isJiren, byte... gender) {
        final Pet pet;
        synchronized (player) {
            // Prepare completely before removing the current pet; saves never see an empty slot.
            pet = createNewPet(player, isMabu, isUub, isKidBeer, isJiren, gender);
            Pet previous = player.pet;
            if (previous != null) {
                if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                    previous.unFusion();
                }
                ChangeMapService.gI().exitMap(previous);
                previous.dispose();
            }
            player.pet = pet;
            if (player.isPl()) {
                Service.gI().point(player);
                Service.gI().showInfoPet(player);
            }
        }
        Thread greeting = new Thread(() -> {
            try {
                Thread.sleep(1000);
                synchronized (player) {
                    if (player.pet != pet || pet.master != player) {
                        return;
                    }
                    Service.gI().chatJustForMe(player, pet, isMabu ? "Oa oa oa..."
                            : isKidBeer ? "Hãy hợp tác với ta, Kakarot!"
                            : "Xin hãy thu nhận tao làm đệ tử");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "pet-greeting");
        greeting.setDaemon(true);
        greeting.start();
    }

    public void changeNormalPet(Player player, int gender) {
        createNormalPet(player, gender);
    }

    public void changeNormalPet(Player player) {
        createNormalPet(player);
    }

    public void changeMabuPet(Player player) {
        createMabuPet(player);
    }

    public void changeUubPet(Player player) {
        createUubPet(player);
    }

    public void changeKidBeerPet(Player player) {
        createKidBeerPet(player);
    }

    public void changeJirenPet(Player player) {
        createJirenPet(player);
    }

    public void changeMabuPet(Player player, int gender) {
        createMabuPet(player, gender);
    }

    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            } else if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            } else if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception e) {
                }
            }).start();
        } catch (Exception ex) {

        }
    }

    private int[] getDataPetNormal() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(20, 45); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetMabu() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(50, 120); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetUub() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetKidBeer() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }
    
    private int[] getDataPetJiren() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    public static String defaultPetName(int type) {
        return switch (type) {
            case 0 -> "$Đệ tử";
            case 1 -> "$Mabư";
            case 2 -> "$Uub";
            case 3 -> "$Kid Beer";
            case 4 -> "$Kid Jiren";
            default -> throw new IllegalArgumentException("Invalid pet type");
        };
    }

    public Pet createNewPet(Player player, boolean isMabu, boolean isUub, boolean isKidBeer, boolean isJiren, byte... gender) {
        int[] data;

        if (isMabu) {
            data = getDataPetMabu();
        } else if (isUub) {
            data = getDataPetUub();
        } else if (isKidBeer) {
            data = getDataPetKidBeer();
        } else if (isJiren) {
            data = getDataPetJiren();
        } else {
            data = getDataPetNormal();
        }

        Pet pet = new Pet(player);

        pet.name = defaultPetName(isMabu ? 1 : isUub ? 2 : isKidBeer ? 3 : isJiren ? 4 : 0);

        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);

        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;

        pet.nPoint.power = isUub ? 40000000000L : isMabu ? 1500000L : isKidBeer ? 40000000000L : isJiren ? 40000000000L : 2000L;

        pet.typePet = (byte) (isMabu ? 1 : isUub ? 2 : isKidBeer ? 3 : isJiren ? 4 : 0);
        pet.type = pet.typePet;

        pet.nPoint.limitPower = 0;
        pet.restoreMinimumPowerLimit();
        pet.nPoint.tiemNang = 0;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];

        int itemBodySize = 7;
        if (pet.typePet == 2 || pet.typePet == 3 || pet.typePet == 4) {
            itemBodySize = 9;
        }
        for (int i = 0; i < itemBodySize; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }

        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 6; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }

        PetSkillRules.ensureSlots(pet);
        pet.nPoint.setBasePoint();
        pet.nPoint.setFullHpMp();
        return pet;
    }

    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newPet != null) {
            pl.newPet.dispose();
        }
        pl.newPet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newPet.name = "$";
        pl.newPet.gender = pl.gender;
        pl.newPet.nPoint.tiemNang = 1;
        pl.newPet.nPoint.power = 1;
        pl.newPet.nPoint.limitPower = 1;
        pl.newPet.nPoint.hpg = 500000000;
        pl.newPet.nPoint.mpg = 500000000;
        pl.newPet.nPoint.hp = 500000000;
        pl.newPet.nPoint.mp = 500000000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;
        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
    }
}
