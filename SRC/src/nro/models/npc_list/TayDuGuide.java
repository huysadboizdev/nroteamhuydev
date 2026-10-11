package nro.models.npc_list;

import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.NguHanhSonService;
import nro.models.services.NguHanhSonService.Offer;
import nro.models.utils.Util;

/** Village entrance and the two guides at the final mountain. */
public final class TayDuGuide extends Npc {
    private static final int EXCHANGE_MENU=124049;
    private static final int CONFIRM_START=125000;
    public TayDuGuide(int mapId,int status,int cx,int cy,int template,int avatar){
        super(mapId,status,cx,cy,template,avatar);
    }
    private boolean nearby(Player p){
        return p!=null&&p.zone!=null&&p.location!=null&&p.zone.map.mapId==mapId
            &&!p.isDie()&&Util.getDistance(cx,cy,p.location.x,p.location.y)<=60&&canOpenNpc(p);
    }
    private List<Offer> offers(){return NguHanhSonService.OFFERS.stream()
        .filter(o->NguHanhSonService.ownsOffer(tempId,o.itemId())).toList();}
    @Override public void openBaseMenu(Player p){
        if(!nearby(p))return;
        if(tempId==ConstNpc.DUONG_TANG&&NguHanhSonService.isVillage(mapId))
            createOtherMenu(p,ConstNpc.BASE_MENU,"Ngũ Hành Sơn gồm 3 map, TNSM x5. Đánh quái nhặt Hồng Đào, đến map cuối gặp ta và Ngộ Không để đổi cải trang.","Vào Ngũ Hành Sơn","Đóng");
        else if(mapId==124)
            createOtherMenu(p,ConstNpc.BASE_MENU,tempId==ConstNpc.NGO_KHONG
                ?"Ta đang bị phong ấn dưới Ngũ Hành Sơn. Mang đào đến đây, ta đổi cho ngươi cải trang Ngộ Không chỉ số ngẫu nhiên, hạn dùng 3–7 ngày."
                :"Mang đào chín đến đổi cải trang Bát Giới chỉ số ngẫu nhiên, hạn dùng 3–7 ngày. Cả 3 map đều có TNSM x5.","Đổi cải trang","Về làng","Đóng");
    }
    @Override public void confirmMenu(Player p,int select){
        if(!nearby(p)||select<0)return;
        int menu=p.idMark.getIndexMenu();
        if(menu==ConstNpc.BASE_MENU){
            if(NguHanhSonService.isVillage(mapId)&&tempId==ConstNpc.DUONG_TANG&&select==0){
                p.idMark.setIndexMenu(-1);NguHanhSonService.enter(p);
            }else if(mapId==124){
                if(select==0){
                    List<Offer> offers=offers();String[] labels=new String[offers.size()+1];
                    for(int i=0;i<offers.size();i++)labels[i]=NguHanhSonService.priceLabel(offers.get(i));
                    labels[offers.size()]="Đóng";
                    createOtherMenu(p,EXCHANGE_MENU,"Chỉ số ngẫu nhiên: sức đánh 1–30%, giảm sát thương 1–50%, HP/KI/TNSM/vàng rơi/tốc độ chạy 1–100%. Chống lạnh, hạn dùng ngẫu nhiên 3–7 ngày. Hãy để trống 1 ô hành trang.",labels);
                }else if(select==1){p.idMark.setIndexMenu(-1);NguHanhSonService.returnVillage(p);}
            }
        }else if(menu==EXCHANGE_MENU&&mapId==124){
            List<Offer> offers=offers();if(select>=offers.size()){p.idMark.setIndexMenu(-1);return;}
            Offer o=offers.get(select);
            createOtherMenu(p,CONFIRM_START+o.itemId(),"Đổi "+o.price()+" "+(o.currency()==541?"Quả Hồng Đào":"Quả Hồng Đào Chín")
                +" lấy "+o.name()+" chỉ số ngẫu nhiên, hạn dùng 3–7 ngày?","Đổi","Hủy");
        }else if(menu>=CONFIRM_START&&mapId==124){
            p.idMark.setIndexMenu(-1);int itemId=menu-CONFIRM_START;
            if(select==0&&NguHanhSonService.ownsOffer(tempId,itemId))NguHanhSonService.exchange(p,itemId);
        }
    }
}
