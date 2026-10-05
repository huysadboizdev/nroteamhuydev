package nro.models.services;

import nro.models.consts.ConstNpc;
import nro.models.intrinsic.Intrinsic;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.utils.Util;
import java.util.List;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.function.IntUnaryOperator;
import nro.models.network.MySession;
import nro.models.shop_ky_gui.ConsignTransactions;
import nro.models.item.Item;
import nro.models.server.Manager;

/**
 *
 * @author By Mr Blue
 *
 */
public class IntrinsicService {

    private static IntrinsicService I;
    private static final int[] COST_OPEN = {10, 20, 40, 80, 160, 320, 640, 1280};
    private record MaxChoice(WeakReference<MySession> session,byte gender,long created){}
    private final Map<Player,MaxChoice> maxChoices=Collections.synchronizedMap(new WeakHashMap<>());

    public List<Intrinsic> maxPool(byte gender) {
        if(gender<0||gender>2)throw new IllegalArgumentException("Invalid planet");
        Map<Integer,Intrinsic> unique=new TreeMap<>();
        for(Intrinsic i:Manager.INTRINSICS)if(i!=null&&i.id>0&&(i.gender==gender||i.gender==3)) {
            if(i.paramFrom1<0||i.paramTo1<i.paramFrom1||i.paramFrom2<0||i.paramTo2<i.paramFrom2)
                throw new IllegalStateException("Invalid intrinsic range");
            Intrinsic old=unique.putIfAbsent(i.id,i);
            if(old!=null&&(old.gender!=i.gender||old.paramFrom1!=i.paramFrom1||old.paramTo1!=i.paramTo1
                    ||old.paramFrom2!=i.paramFrom2||old.paramTo2!=i.paramTo2||old.icon!=i.icon||!Objects.equals(old.name,i.name)))
                throw new IllegalStateException("Conflicting intrinsic templates");
        }
        if(unique.isEmpty())throw new IllegalStateException("Empty intrinsic pool");
        return new ArrayList<>(unique.values());
    }
    public Intrinsic rollMax(byte gender,IntUnaryOperator random) {
        List<Intrinsic> pool=maxPool(gender);int index=random.applyAsInt(pool.size());
        if(index<0||index>=pool.size())throw new IllegalArgumentException("Invalid random index");
        Intrinsic result=new Intrinsic(pool.get(index));result.param1=result.paramTo1;result.param2=result.paramTo2;return result;
    }
    public void showConfirmOpenMax(Player p) {
        synchronized(p) {
            maxChoices.remove(p);
            if(p.session==null||AdminPlayerActions.hasPending(p)){
                Service.gI().sendThongBao(p,"Đang có giao dịch chờ xử lý. Vui lòng thử lại sau.");return;
            }
            maxChoices.put(p,new MaxChoice(new WeakReference<>(p.session),p.gender,System.currentTimeMillis()));
            NpcService.gI().createMenuConMeo(p,ConstNpc.CONFIRM_OPEN_INTRINSIC_MAX,-1,
                    "Random nội tại đúng hành tinh, chỉ số tối đa.\nGiá 100 hồng ngọc/lần. Yêu cầu sức mạnh 10 tỷ.","Mở max","Từ chối");
        }
    }
    public void confirmMax(Player p,int select) {
        synchronized(p) {
            MaxChoice choice=maxChoices.remove(p);
            if(select!=0)return;
            if(choice==null||choice.session.get()!=p.session||choice.gender!=p.gender||System.currentTimeMillis()-choice.created>120_000){
                Service.gI().sendThongBao(p,"Lựa chọn hết hạn. Hãy mở lại menu nội tại max.");return;
            }
            try {
                Intrinsic reward=rollMax(p.gender,bound->Util.nextInt(0,bound-1));
                var result=transactions().purchaseIntrinsicMax(p,reward);
                // Notification failure after commit must never trigger another roll or debit.
                try{Service.gI().sendThongBao(p,result.message());}catch(Exception ignored){}
                if(result.success()){
                    try{sendInfoIntrinsic(p);}catch(Exception ignored){}
                    try{Service.gI().sendMoney(p);}catch(Exception ignored){}
                    try{Service.gI().point(p);}catch(Exception ignored){}
                }
            }catch(RuntimeException invalid){
                nro.models.utils.Logger.logException(IntrinsicService.class,invalid,"Invalid max intrinsic configuration");
                Service.gI().sendThongBao(p,"Cấu hình nội tại chưa hợp lệ; chưa trừ hồng ngọc.");
            }
        }
    }
    protected ConsignTransactions transactions(){return new ConsignTransactions();}

    public static IntrinsicService gI() {
        if (IntrinsicService.I == null) {
            IntrinsicService.I = new IntrinsicService();
        }
        return IntrinsicService.I;
    }

    public List<Intrinsic> getIntrinsics(byte playerGender) {
        switch (playerGender) {
            case 0:
                return Manager.INTRINSIC_TD;
            case 1:
                return Manager.INTRINSIC_NM;
            default:
                return Manager.INTRINSIC_XD;
        }
    }

    public Intrinsic getIntrinsicById(int id) {
        for (Intrinsic intrinsic : Manager.INTRINSICS) {
            if (intrinsic.id == id) {
                return new Intrinsic(intrinsic);
            }
        }
        return null;
    }

    public void sendInfoIntrinsic(Player player) {
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(0);
            msg.writer().writeShort(player.playerIntrinsic.intrinsic.icon);
            msg.writer().writeUTF(player.playerIntrinsic.intrinsic.getName());
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showAllIntrinsic(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(1);
            msg.writer().writeByte(1); //count tab
            msg.writer().writeUTF("Nội tại");
            msg.writer().writeByte(listIntrinsic.size() - 1);
            for (int i = 1; i < listIntrinsic.size(); i++) {
                msg.writer().writeShort(listIntrinsic.get(i).icon);
                msg.writer().writeUTF(listIntrinsic.get(i).getDescription());
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showMenu(Player player) {
        maxChoices.remove(player);
        NpcService.gI().createMenuConMeo(player, ConstNpc.INTRINSIC, -1,
                "Nội tại là một kỹ năng bị động hỗ trợ đặc biệt\nBạn có muốn mở hoặc thay đổi nội tại không?",
                "Xem\ntất cả\nNội Tại", "Mở\nNội Tại", "Mở VIP", "Mở max", "Từ chối");
    }

    public void showConfirmOpen(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC, -1, "Bạn muốn đổi Nội Tại khác\nvới giá là "
                + COST_OPEN[player.playerIntrinsic.countOpen] + " Tr vàng ?", "Mở\nNội Tại", "Từ chối");
    }

    public void showConfirmOpenVip(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP, -1,
                "Bạn có muốn mở Nội Tại\nvới giá là 100 ngọc và\ntái lập giá vàng quay lại ban đầu không?", "Mở\nNội VIP", "Từ chối");
    }

    private void changeIntrinsic(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        player.playerIntrinsic.intrinsic = new Intrinsic(listIntrinsic.get(Util.nextInt(1, listIntrinsic.size() - 1)));
        player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom1, player.playerIntrinsic.intrinsic.paramTo1);
        player.playerIntrinsic.intrinsic.param2 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom2, player.playerIntrinsic.intrinsic.paramTo2);
        String name = player.playerIntrinsic.intrinsic.getName();
        int index = name.indexOf(" [");

        if (index != -1) {
            Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + name.substring(0, index));
        } else {
            Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + name);
        }

        sendInfoIntrinsic(player);
    }

    public void doinoitai(Player player) {
        synchronized(player){if(blocked(player))return;doChange(player);}
    }
    private boolean blocked(Player player) {
        if(player==null||player.session==null||AdminPlayerActions.hasPending(player)){
            if(player!=null)Service.gI().sendThongBao(player,"Đang có giao dịch chờ xử lý. Vui lòng thử lại sau.");return true;
        }return false;
    }
    private void doChange(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        player.playerIntrinsic.intrinsic = new Intrinsic(listIntrinsic.get(Util.nextInt(1, listIntrinsic.size() - 1)));
        player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom1, player.playerIntrinsic.intrinsic.paramTo1);
        player.playerIntrinsic.intrinsic.param2 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom2, player.playerIntrinsic.intrinsic.paramTo2);
        Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + player.playerIntrinsic.intrinsic.getName().substring(0, player.playerIntrinsic.intrinsic.getName().indexOf(" [")));
        sendInfoIntrinsic(player);
    }

    public void open(Player player) {
        synchronized(player){if(blocked(player))return;openNormal(player);}
    }
    private void openNormal(Player player) {
        if (player.nPoint.power >= 10000000000L) {
            int goldRequire = COST_OPEN[player.playerIntrinsic.countOpen] * 1000000;
            if (player.inventory.gold >= goldRequire) {
                player.inventory.gold -= goldRequire;
                PlayerService.gI().sendInfoHpMpMoney(player);
                changeIntrinsic(player);
                if (player.playerIntrinsic.countOpen < COST_OPEN.length - 1) {
                    player.playerIntrinsic.countOpen++;
                }
            } else {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu "
                        + Util.numberToMoney(goldRequire - player.inventory.gold) + " vàng nữa");
            }
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu 10 tỷ");
        }
    }

    public void openVip(Player player) {
        synchronized(player){if(blocked(player))return;openVipNormal(player);}
    }
    private void openVipNormal(Player player) {
        if (player.nPoint.power >= 10000000000L) {
            int gemRequire = 100;
            if (player.inventory.gem >= 100) {
                player.inventory.gem -= gemRequire;
                PlayerService.gI().sendInfoHpMpMoney(player);
                changeIntrinsic(player);
                player.playerIntrinsic.countOpen = 0;
            } else {
                Service.gI().sendThongBao(player, "Bạn không có đủ ngọc, còn thiếu "
                        + (gemRequire - player.inventory.gem) + " ngọc nữa");
            }
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu 10 tỷ");
        }
    }

}
