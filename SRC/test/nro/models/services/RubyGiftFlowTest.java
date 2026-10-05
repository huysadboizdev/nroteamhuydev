package nro.models.services;

import java.lang.reflect.Method;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.network.Message;
import nro.models.services_func.Input;
import static nro.models.services.PinkFixture.*;

public final class RubyGiftFlowTest {
    public static void main(String[] args)throws Exception {
        try {
            install();Player p=player(870);p.name="fixture";p.idMark=new IDMark();
            p.nPoint=blank(NPoint.class);p.nPoint.power=Long.MAX_VALUE;
            Item t=ItemService.gI().createNewItem((short)2000,2);p.inventory.itemsBag.add(t);
            Method use=nro.models.services_func.UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);
            use.setAccessible(true);use.invoke(nro.models.services_func.UseItem.gI(),p,t,0);
            check(p.idMark.getTypeInput()==62001,"ticket opens dedicated ruby gift form");
            check(t.quantity==2&&p.inventory.ruby==0&&p.inventory.gem==0,"opening form consumes nothing");
            p.idMark.setTypeInput(62002);Message out=new Message(-125);out.writer().writeByte(1);out.writer().writeUTF("0");
            Input.gI().doInput(p,new Message((byte)-125,out.getData()));
            check(p.idMark.getTypeInput()==-1&&t.quantity==2,"cancel invalidates confirmation without consuming ticket");
            p.idMark.setTypeInput(62001);out=new Message(-125);out.writer().writeByte(2);out.writer().writeUTF("fixture");out.writer().writeUTF("2147483648");
            Input.gI().doInput(p,new Message((byte)-125,out.getData()));
            check(t.quantity==2&&p.inventory.ruby==0,"overflow input consumes nothing");
            System.out.println("PASS RubyGiftFlow");System.exit(0);
        } catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
