package nro.models.server;

import nro.models.services.PlayerService;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.ConstNpc;
import nro.models.managers.GiftCodeManager;
import nro.models.item.Item;
import java.util.ArrayList;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.NpcService;
import nro.models.services.InventoryService;
import nro.models.utils.SystemMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nro.models.Bot.BotAttackplayer;
import nro.models.Bot.BotManager;
import nro.models.consts.ConstPlayer;
import nro.models.data.LocalManager;
import nro.models.services.TaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Command {

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
        adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("boss", player -> nro.models.boss.Boss_Manager.BossManager.gI().showListBoss(player));
        adminCommands.put("brl", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("hs", nro.models.services.AdminCommandService::recoverSelf);
        adminCommands.put("kol", nro.models.services.AdminCommandService::completeKol);
        adminCommands.put("d", player -> Service.gI().setPos(player, player.location.x, player.location.y + 10));
        adminCommands.put("a", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time start: " + ServerManager.timeStart
                + "\nClients: " + Client.gI().getPlayers().size()
                + "\n Sessions: " + SessionManager.gI().getNumSession()
                + "\nThreads: " + Thread.activeCount()
                + " luồng" + "\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Đóng"));
    }

    private void initParameterizedCommands() {
        parameterizedCommands.put("1", (player, text) -> {
            NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                    + "Player online : " + Client.gI().getPlayers().size() + "\n"
                    + "\b|1|Thread: " + Thread.activeCount() + "\n"
                    + "\n Sessions: " + SessionManager.gI().getNumSession() + "\n"
                    + "Bot online : " + BotManager.gI().bot.size(),
                    "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Bot\nAttack Player");
            return;
        });

        parameterizedCommands.put("2", (player, text) -> {
            player.originalName = player.name;
            PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_ALL);
            player.originalName = player.name;
            Service.gI().Send_Caitrang(player);
            BotAttackplayer bot = new BotAttackplayer((short) 1624, (short) 1628, (short) 1629, 1, "đánh nhau không?", (short) 0);
            bot.player = player;
            bot.zone = player.zone;
            bot.location.x = player.location.x;
            bot.location.y = player.location.y;

            player.zone.addPlayer(bot);
            BotManager.gI().bot.add(bot);

            for (Player p : player.zone.getPlayers()) {
                if (p.session != null) {
                    Service.gI().sendAppear(bot, p);
                    Service.gI().sendInfoCharMoiToMe(p, bot);
                }
            }

            if (player.session != null) {
                Service.gI().Send_Info_NV(player);
            }

            bot.update();

            ServerNotify.gI().notify("Đã gọi bot tấn công người chơi!");
        });

        parameterizedCommands.put("b", (player,text) -> Input.gI().createFormSenditem1(player));
        parameterizedCommands.put("toado", (player,text) -> Service.gI().sendThongBaoOK(player,
                "x: " + player.location.x + " - y: " + player.location.y));
        parameterizedCommands.put("m", this::moveMap);
        parameterizedCommands.put("n", this::assignTask);
        parameterizedCommands.put("dm", (p,t) -> setStat(p,t,"dm"));
        parameterizedCommands.put("dmg", parameterizedCommands.get("dm"));
        parameterizedCommands.put("hp", (p,t) -> setStat(p,t,"hp"));
        parameterizedCommands.put("hpg", parameterizedCommands.get("hp"));
        parameterizedCommands.put("ki", (p,t) -> setStat(p,t,"ki"));
        parameterizedCommands.put("up", (p,t) -> increasePower(p,t,false));
        parameterizedCommands.put("upp", (p,t) -> increasePower(p,t,true));
        parameterizedCommands.put("i", this::giveItem);
    }

    private String[] args(String text) { return text.trim().split("\\s+"); }
    private long positiveValue(String text) {
        String[] args=args(text);
        if(args.length!=2) throw new IllegalArgumentException();
        long value=Long.parseLong(args[1]);
        if(value<=0) throw new IllegalArgumentException();
        return value;
    }
    private void setStat(Player player,String text,String stat) {
        try {
            long value=positiveValue(text);
            int limit=stat.equals("dm") ? player.nPoint.getDameLimit() : player.nPoint.getHpMpLimit();
            if(value>limit) throw new IllegalArgumentException();
            if(!nro.models.services.AdminCommandService.requireAdmin(player)) return;
            switch(stat) { case "dm" -> player.nPoint.dameg=(int)value;
                case "hp" -> player.nPoint.hpg=(int)value; case "ki" -> player.nPoint.mpg=(int)value; }
            Service.gI().point(player);
            Service.gI().sendThongBao(player,"Đã đặt " + stat + " = " + value);
        } catch(IllegalArgumentException e) { Service.gI().sendThongBao(player,"Cú pháp: " + stat + " <số dương trong giới hạn chỉ số>"); }
    }
    private void increasePower(Player player,String text,boolean pet) {
        try {
            long value=positiveValue(text);
            Player target=pet ? player.pet : player;
            if(target==null) { Service.gI().sendThongBao(player,"Bạn chưa có đệ tử"); return; }
            synchronized (player) {
                if(target.nPoint.power<0 || target.nPoint.tiemNang<0
                        || value>Long.MAX_VALUE-target.nPoint.power || value>Long.MAX_VALUE-target.nPoint.tiemNang)
                    throw new IllegalArgumentException();
                if(pet && player.nPoint.power < player.nPoint.getPowerLimit()) {
                    long masterShare=player.nPoint.calSubTNSM((long)(value*0.5));
                    masterShare=Math.min(masterShare,player.nPoint.getPowerLimit()-player.nPoint.power);
                    if(masterShare > (Long.MAX_VALUE-player.nPoint.tiemNang)/2) throw new IllegalArgumentException();
                }
                if(!nro.models.services.AdminCommandService.requireAdmin(player)) return;
                Service.gI().addSMTN(target,(byte)2,value,false);
            }
            Service.gI().sendThongBao(player,"Đã yêu cầu tăng SMTN " + value + (pet ? " cho đệ tử" : ""));
        } catch(IllegalArgumentException e) { Service.gI().sendThongBao(player,"Cú pháp: " + (pet ? "upp" : "up") + " <số dương không tràn số>"); }
    }
    private void assignTask(Player player,String text) {
        try {
            String[] parts=args(text);
            if(parts.length!=2) throw new IllegalArgumentException();
            int id=Integer.parseInt(parts[1]);
            if(id<0 || nro.models.server.Manager.TASKS.stream().noneMatch(t->t.id==id))
                throw new IllegalArgumentException();
            var target=TaskService.gI().getTaskMainById(player,(int)id);
            if(target==null || target.id!=id || target.subTasks==null || target.subTasks.isEmpty())
                throw new IllegalArgumentException();
            if(!nro.models.services.AdminCommandService.requireAdmin(player)) return;
            target.index=0; target.subTasks.forEach(t->t.count=0);
            player.playerTask.taskMain=target;
            TaskService.gI().sendTaskMain(player);
            Service.gI().sendThongBao(player,"Đã đặt nhiệm vụ " + id);
        } catch(IllegalArgumentException e) { Service.gI().sendThongBao(player,"Cú pháp: n <task ID hợp lệ>"); }
    }
    private void moveMap(Player player,String text) {
        try {
            String[] parts=args(text);
            if(parts.length<2 || parts.length>3) throw new IllegalArgumentException();
            int mapId=Integer.parseInt(parts[1]); int zoneId=parts.length==3 ? Integer.parseInt(parts[2]) : -1;
            var map=nro.models.map.service.MapService.gI().getMapById(mapId);
            if(map==null || map.tileMap==null || map.tileTop==null || map.mapWidth<=0
                    || zoneId < -1 || (zoneId>=0 && map.zones.stream().noneMatch(z->z.zoneId==zoneId)))
                throw new IllegalArgumentException();
            if(player.location==null) throw new IllegalArgumentException();
            int originalX=player.location.x, originalY=player.location.y;
            nro.models.map.Zone zone;
            try {
                zone=nro.models.map.service.MapService.gI().getMapCanJoin(player,mapId,zoneId);
            } finally {
                // Legacy dungeon fallback resolver changes coordinates before choosing its map.
                player.location.x=originalX;
                player.location.y=originalY;
            }
            if(zone==null || zone.map==null || zone.map.mapId!=mapId || (zoneId>=0 && zone.zoneId!=zoneId)) {
                Service.gI().sendThongBao(player,"Không thể vào đúng map/khu đã chọn.");return;
            }
            if(!nro.models.services.AdminCommandService.requireAdmin(player)) return;
            int x=Math.min(100,map.mapWidth-1);
            ChangeMapService.gI().changeMapInYard(player,zone,x);
        } catch(IllegalArgumentException e) { Service.gI().sendThongBao(player,"Map/khu không hợp lệ hoặc tile chưa nạp. Cú pháp: m <map ID> [khu]"); }
    }
    private void giveItem(Player player,String text) {
        try {
            String[] parts=args(text);
            if(parts.length<2) throw new IllegalArgumentException();
            int id=Integer.parseInt(parts[1]); int quantity=parts.length>=3 ? Integer.parseInt(parts[2]) : 1;
            List<Item.ItemOption> options=new ArrayList<>();
            for(int i=3;i<parts.length;i++) {
                String[] option=parts[i].split(":",-1);
                if(option.length!=2) throw new IllegalArgumentException();
                options.add(nro.models.services.AdminCommandService.validateOption(Integer.parseInt(option[0]),Integer.parseInt(option[1])));
            }
            int accepted=nro.models.services.AdminCommandService.grantItems(player,player,id,quantity,options);
            Service.gI().sendThongBao(player,accepted>0 ? "Đã nhận " + accepted + "/" + quantity + " vật phẩm [" + id + "]"
                    : "Không cấp được vật phẩm: hành trang đầy hoặc giao dịch đang chờ.");
        } catch(IllegalArgumentException|IndexOutOfBoundsException e) { Service.gI().sendThongBao(player,"Lỗi cú pháp/catalog! Dùng: i <itemId> <số lượng dương> [optionId:value]"); }
    }

    public void chat(Player player, String text) {
        String cleanedText = text.trim();
        if (cleanedText.isEmpty()) {
            return;
        }
        if (!check(player, cleanedText)) {
            Service.gI().chat(player, cleanedText);
        }
    }

    public boolean check(Player player, String text) {
        if (text == null || player == null) return false;
        text=text.trim();
        String token=text.isEmpty() ? "" : args(text)[0];
        if (nro.models.services.AdminCommandService.isAuthorized(player)) {
            try {
                Consumer<Player> simple=adminCommands.get(token);
                if(simple!=null) {
                    if(!token.equals(text)) Service.gI().sendThongBao(player,"Cú pháp: " + token);
                    else if(nro.models.services.AdminCommandService.requireAdmin(player)) simple.accept(player);
                    return true;
                }
                BiConsumer<Player,String> parameterized=parameterizedCommands.get(token);
                if(parameterized!=null) {
                    if(nro.models.services.AdminCommandService.requireAdmin(player)) parameterized.accept(player,text);
                    return true;
                }
            } catch(Exception e) {
                nro.models.utils.Logger.logException(Command.class,e);
                Service.gI().sendThongBao(player,"Không thể thực hiện lệnh admin. Hãy thử lại.");
                return true;
            }
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replace("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
                case "sach tuyet ky" -> {
                    int typePet = player.pet.typePet;
                    if (typePet == 2 || typePet == 3 || typePet == 4) {
                        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                            Item item = player.inventory.itemsBag.get(i);
                            if (item != null && item.isNotNullItem() && item.template.type == 25) {
                                if (player.pet.nPoint != null && player.pet.nPoint.power >= 1500000) {
                                    Item old = InventoryService.gI().putItemBody(player.pet, item);
                                    player.inventory.itemsBag.set(i, old);
                                    InventoryService.gI().sendItemBags(player);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.gI().Send_Caitrang(player.pet);
                                    Service.gI().Send_Caitrang(player);
                                    Service.gI().sendThongBao(player, "Đã dùng " + item.template.name + " cho đệ tử");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                                }
                                break;
                            }
                        }
                    } else {
                        Service.gI().sendThongBaoOK(player, "Chỉ đệ tử (Goku vô cực, Kid Beerus, Jiren) mới có thể dùng sách tuyệt kỹ.");
                    }
                }
            }
        }
        return false;
    }
}
