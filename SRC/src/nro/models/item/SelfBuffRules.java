package nro.models.item;

import java.util.function.*;

/** Extends the existing start-time representation without changing saved JSON indexes. */
public enum SelfBuffRules {
 CUONG_NO(381,ItemTime.TIME_ITEM,t->t.isUseCuongNo,t->t.lastTimeCuongNo,(t,v)->t.lastTimeCuongNo=v,(t,v)->t.isUseCuongNo=v),
 BO_HUYET(382,ItemTime.TIME_ITEM,t->t.isUseBoHuyet,t->t.lastTimeBoHuyet,(t,v)->t.lastTimeBoHuyet=v,(t,v)->t.isUseBoHuyet=v),
 BO_KHI(383,ItemTime.TIME_ITEM,t->t.isUseBoKhi,t->t.lastTimeBoKhi,(t,v)->t.lastTimeBoKhi=v,(t,v)->t.isUseBoKhi=v),
 GIAP_XEN(384,ItemTime.TIME_ITEM,t->t.isUseGiapXen,t->t.lastTimeGiapXen,(t,v)->t.lastTimeGiapXen=v,(t,v)->t.isUseGiapXen=v),
 AN_DANH(385,ItemTime.TIME_ITEM,t->t.isUseAnDanh,t->t.lastTimeAnDanh,(t,v)->t.lastTimeAnDanh=v,(t,v)->t.isUseAnDanh=v),
 CUONG_NO_2(1150,ItemTime.TIME_ITEM,t->t.isUseCuongNo2,t->t.lastTimeCuongNo2,(t,v)->t.lastTimeCuongNo2=v,(t,v)->t.isUseCuongNo2=v),
 BO_KHI_2(1151,ItemTime.TIME_ITEM,t->t.isUseBoKhi2,t->t.lastTimeBoKhi2,(t,v)->t.lastTimeBoKhi2=v,(t,v)->t.isUseBoKhi2=v),
 BO_HUYET_2(1152,ItemTime.TIME_ITEM,t->t.isUseBoHuyet2,t->t.lastTimeBoHuyet2,(t,v)->t.lastTimeBoHuyet2=v,(t,v)->t.isUseBoHuyet2=v),
 GIAP_XEN_2(1153,ItemTime.TIME_ITEM,t->t.isUseGiapXen2,t->t.lastTimeGiapXen2,(t,v)->t.lastTimeGiapXen2=v,(t,v)->t.isUseGiapXen2=v),
 AN_DANH_2(1154,ItemTime.TIME_ITEM,t->t.isUseAnDanh2,t->t.lastTimeAnDanh2,(t,v)->t.lastTimeAnDanh2=v,(t,v)->t.isUseAnDanh2=v),
 MEAL(663,ItemTime.TIME_EAT_MEAL,t->t.isEatMeal,t->t.lastTimeEatMeal,(t,v)->t.lastTimeEatMeal=v,(t,v)->t.isEatMeal=v),
 MEAL_2(880,ItemTime.TIME_EAT_MEAL,t->t.isEatMeal2,t->t.lastTimeEatMeal2,(t,v)->t.lastTimeEatMeal2=v,(t,v)->t.isEatMeal2=v),
 NUOC_MIA_1(1614,ItemTime.TIME_NUOC_MIA1,t->t.isUseNuocMia1,t->t.lastTimeUseNuocMia1,(t,v)->t.lastTimeUseNuocMia1=v,(t,v)->t.isUseNuocMia1=v),
 NUOC_MIA_2(1615,ItemTime.TIME_NUOC_MIA2,t->t.isUseNuocMia2,t->t.lastTimeUseNuocMia2,(t,v)->t.lastTimeUseNuocMia2=v,(t,v)->t.isUseNuocMia2=v),
 NUOC_MIA_3(1616,ItemTime.TIME_NUOC_MIA3,t->t.isUseNuocMia3,t->t.lastTimeUseNuocMia3,(t,v)->t.lastTimeUseNuocMia3=v,(t,v)->t.isUseNuocMia3=v),
 CMS(638,ItemTime.TIME_CMS,t->t.isUseCMS,t->t.lastTimeUseCMS,(t,v)->t.lastTimeUseCMS=v,(t,v)->t.isUseCMS=v),
 NOI_COM(1233,ItemTime.TIME_NCD,t->t.isUseNCD,t->t.lastTimeUseNCD,(t,v)->t.lastTimeUseNCD=v,(t,v)->t.isUseNCD=v),
 DUOI_KHI(579,ItemTime.TIME_DK,t->t.isUseDK,t->t.lastTimeUseDK,(t,v)->t.lastTimeUseDK=v,(t,v)->t.isUseDK=v),
 CO_BON_LA(1635,ItemTime.TIME_CO_BON_LA,t->t.isUseCoBonLa,t->t.lastTimeUseCoBonLa,(t,v)->t.lastTimeUseCoBonLa=v,(t,v)->t.isUseCoBonLa=v),
 BUA_SANTA(1628,ItemTime.TIME_BUA_SANTA,t->t.isUseBuaSanta,t->t.lastTimeBuaSanta,(t,v)->t.lastTimeBuaSanta=v,(t,v)->t.isUseBuaSanta=v);

 public static final long MAX_DURATION=500*60_000L;
 private final int id;
 public final long duration;
 private final Predicate<ItemTime> active;
 private final ToLongFunction<ItemTime> start;
 private final ObjLongConsumer<ItemTime> setStart;
 private final BiConsumer<ItemTime,Boolean> setActive;
 SelfBuffRules(int id,long duration,Predicate<ItemTime> active,ToLongFunction<ItemTime> start,ObjLongConsumer<ItemTime> setStart,BiConsumer<ItemTime,Boolean> setActive){this.id=id;this.duration=duration;this.active=active;this.start=start;this.setStart=setStart;this.setActive=setActive;}
 public static SelfBuffRules find(int id){
  if(id>=663&&id<=667)return MEAL;
  if(id>=880&&id<=882)return MEAL_2;
  if(id==1045)return DUOI_KHI;
  for(SelfBuffRules r:values())if(r.id==id)return r;
  return null;
 }
 public long remaining(ItemTime t,long now){return active.test(t)?Math.max(0,start.applyAsLong(t)+duration-now):0;}
 public static void clearExpired(ItemTime t,long now){for(SelfBuffRules r:values())if(r.active.test(t)&&r.remaining(t,now)==0)r.setActive.accept(t,false);}
 /** Negative means the next complete dose exceeds the cap. */
 public long nextStart(ItemTime t,long now){long total=remaining(t,now)+duration;return total>MAX_DURATION?-1:now+total-duration;}
 public boolean sameFood(ItemTime t,Item item,long now){return remaining(t,now)==0 || (this!=MEAL&&this!=MEAL_2) || (this==MEAL?t.iconMeal:t.iconMeal2)==item.template.iconID;}
 public void commit(ItemTime t,long nextStart){setStart.accept(t,nextStart);}
}
