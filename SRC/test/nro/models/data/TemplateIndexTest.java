package nro.models.data;
import java.io.IOException;
import java.util.*;
public class TemplateIndexTest {
    public static void main(String[] args) throws Exception {
        List<Integer> rows=TemplateIndex.byId(List.of(110,0,84,103),i->i);
        if(rows.size()!=111||rows.get(85)!=null||rows.get(103)!=103||rows.get(110)!=110)
            throw new AssertionError("ID gaps shifted templates");
        if(!TemplateIndex.byId(List.of(0,1,2),i->i).equals(List.of(0,1,2)))throw new AssertionError("Contiguous templates changed");
        try{TemplateIndex.byId(List.of(1,1),i->i);throw new AssertionError("Duplicate accepted");}catch(IOException expected){}
        try{TemplateIndex.byId(List.of(255),i->i);throw new AssertionError("Byte overflow accepted");}catch(IOException expected){}
        try{TemplateIndex.byId(List.of(-1),i->i);throw new AssertionError("Negative accepted");}catch(IOException expected){}
        System.out.println("TEMPLATE_INDEX=PASS gaps,order,duplicates,bounds");
    }
}
