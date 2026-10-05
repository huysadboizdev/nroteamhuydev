package nro.models.services;

import com.google.gson.JsonArray;
import nro.models.database.FreeMembershipDAO;
import nro.models.player.Player;
import nro.models.task.TaskMain;
import nro.models.utils.Logger;

public final class FreeMembershipService {
    private static final FreeMembershipService INSTANCE=new FreeMembershipService(new FreeMembershipDAO());
    private final FreeMembershipDAO dao;
    public record Result(boolean success,String message){}
    public FreeMembershipService(FreeMembershipDAO dao){this.dao=dao;}
    public static FreeMembershipService gI(){return INSTANCE;}
    public static boolean eligible(Player p){
        return p!=null&&p.playerTask!=null&&p.playerTask.taskMain!=null&&p.playerTask.taskMain.id>=2;
    }
    public Result open(Player p){
        if(p==null)return new Result(false,"Không tìm thấy nhân vật.");
        synchronized(p){
            if(p.session==null||p.session.player!=p||!p.session.isConnected()||p.beforeDispose||p.isOffline)
                return new Result(false,"Nhân vật chưa sẵn sàng. Vui lòng thử lại.");
            if(p.session.actived)return new Result(true,"Tài khoản đã mở thành viên rồi.");
            if(!eligible(p))return new Result(false,"Con cần hoàn thành nhiệm vụ Mộc Nhân và về báo nhiệm vụ trước.");
            if(AdminPlayerActions.hasPending(p))return new Result(false,"Đang có giao dịch chờ xử lý. Vui lòng thử lại sau.");
            TaskMain task=p.playerTask.taskMain;
            if(task.subTasks==null||task.index<0||task.index>=task.subTasks.size()||task.subTasks.get(task.index)==null)
                return new Result(false,"Dữ liệu nhiệm vụ chưa hợp lệ.");
            JsonArray data=new JsonArray();data.add(task.id);data.add(task.index);data.add(task.subTasks.get(task.index).count);data.add(task.lastTime);
            try{
                if(!dao.activate(p.id,p.session.userId,data.toString()))return new Result(false,"Chưa mở được thành viên. Vui lòng thử lại.");
                p.session.actived=true;
                return new Result(true,"Mở thành viên miễn phí thành công.");
            }catch(Exception failure){
                Logger.logException(FreeMembershipService.class,failure,"Free membership activation failed");
                return new Result(false,"Chưa xác nhận được mở thành viên. Vui lòng thử lại, không mất phí.");
            }
        }
    }
}
