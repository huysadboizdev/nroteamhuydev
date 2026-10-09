package nro.models.services;
import java.util.Arrays;
import nro.models.item.CompanionCatalog;
public class AnimeAppearanceTest {
 public static void main(String[] args){
  if(!Arrays.equals(CompanionCatalog.parts(2019),new short[]{2129,2130,2131}))throw new AssertionError("Kurama companion mapping missing");
  short[] first=CompanionCatalog.parts(2019);first[0]=0;
  if(CompanionCatalog.parts(2019)[0]!=2129)throw new AssertionError("mutable catalog");
  if(nro.models.data.DataGame.vsData!=15||nro.models.data.DataGame.vsItem!=20)throw new AssertionError("client reload versions missing");
  System.out.println("ANIME_APPEARANCE PASS companion/version/immutable-parts");
 }
}
