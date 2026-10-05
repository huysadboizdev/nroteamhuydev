package nro.models.services;
import java.nio.file.*;
import com.google.gson.Gson;
import nro.models.intrinsic.Intrinsic;
import nro.models.server.Manager;
public final class IntrinsicMaxCatalogTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        Intrinsic[] rows=new Gson().fromJson(Files.readString(Path.of(args[0])),Intrinsic[].class);
        Manager.INTRINSICS.clear();for(var row:rows)Manager.INTRINSICS.add(row);
        var service=IntrinsicService.gI();
        for(byte gender=0;gender<3;gender++){
            var pool=service.maxPool(gender);check(pool.size()==(gender==1?11:10),"actual SQL pool counts");
            for(int index=0;index<pool.size();index++){
                final int n=index;Intrinsic i=service.rollMax(gender,bound->n);
                check(i.id>0&&(i.gender==gender||i.gender==3),"actual planet and common");
                check(i.param1==i.paramTo1&&i.param2==i.paramTo2,"actual maxima both parameters");
                if(i.id==3)check(i.param1==35&&i.param2==35,"earth two stats35/35");
                if(i.id==23)check(i.param1==300,"shared gold300");
            }
        }
        Intrinsic original=java.util.Arrays.stream(rows).filter(i->i.id>0).findFirst().orElseThrow();
        Intrinsic conflict=new Intrinsic(original);conflict.paramTo1++;Manager.INTRINSICS.add(conflict);
        try{service.maxPool(conflict.gender==3?(byte)0:conflict.gender);throw new AssertionError("conflicting duplicate denied");}catch(IllegalStateException expected){}
        Manager.INTRINSICS.clear();try{service.maxPool((byte)0);throw new AssertionError("empty pool denied");}catch(IllegalStateException expected){}
        try{service.maxPool((byte)3);throw new AssertionError("invalid planet denied");}catch(IllegalArgumentException expected){}
        System.out.println("PASS IntrinsicMaxCatalog: actual SQL pools10/11/10, all max values, ID0 excluded, duplicates/conflicts, empty/invalid pools");
    }
}
