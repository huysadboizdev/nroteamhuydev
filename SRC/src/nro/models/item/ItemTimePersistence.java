package nro.models.item;

import java.util.*;
import java.util.function.*;
import org.json.simple.JSONArray;

/** One schema for timer save/load. Reads both historical layouts without rewriting the database. */
public final class ItemTimePersistence {
 private ItemTimePersistence(){}
 private record Timer(int index,int oldIndex,long duration,Predicate<ItemTime> active,ToLongFunction<ItemTime> start,ObjLongConsumer<ItemTime> setStart,BiConsumer<ItemTime,Boolean> setActive){}
 private static final List<Timer> TIMERS=List.of(
  new Timer(0,0,ItemTime.TIME_ITEM,t->t.isUseBoHuyet,t->t.lastTimeBoHuyet,(t,v)->t.lastTimeBoHuyet=v,(t,v)->t.isUseBoHuyet=v),
  new Timer(1,1,ItemTime.TIME_ITEM,t->t.isUseBoHuyet2,t->t.lastTimeBoHuyet2,(t,v)->t.lastTimeBoHuyet2=v,(t,v)->t.isUseBoHuyet2=v),
  new Timer(2,2,ItemTime.TIME_ITEM,t->t.isUseBoKhi,t->t.lastTimeBoKhi,(t,v)->t.lastTimeBoKhi=v,(t,v)->t.isUseBoKhi=v),
  new Timer(3,3,ItemTime.TIME_ITEM,t->t.isUseBoKhi2,t->t.lastTimeBoKhi2,(t,v)->t.lastTimeBoKhi2=v,(t,v)->t.isUseBoKhi2=v),
  new Timer(4,4,ItemTime.TIME_ITEM,t->t.isUseGiapXen,t->t.lastTimeGiapXen,(t,v)->t.lastTimeGiapXen=v,(t,v)->t.isUseGiapXen=v),
  new Timer(5,5,ItemTime.TIME_ITEM,t->t.isUseGiapXen2,t->t.lastTimeGiapXen2,(t,v)->t.lastTimeGiapXen2=v,(t,v)->t.isUseGiapXen2=v),
  new Timer(6,6,ItemTime.TIME_ITEM,t->t.isUseCuongNo,t->t.lastTimeCuongNo,(t,v)->t.lastTimeCuongNo=v,(t,v)->t.isUseCuongNo=v),
  new Timer(7,7,ItemTime.TIME_ITEM,t->t.isUseCuongNo2,t->t.lastTimeCuongNo2,(t,v)->t.lastTimeCuongNo2=v,(t,v)->t.isUseCuongNo2=v),
  new Timer(8,8,ItemTime.TIME_ITEM,t->t.isUseAnDanh,t->t.lastTimeAnDanh,(t,v)->t.lastTimeAnDanh=v,(t,v)->t.isUseAnDanh=v),
  new Timer(9,9,ItemTime.TIME_ITEM,t->t.isUseAnDanh2,t->t.lastTimeAnDanh2,(t,v)->t.lastTimeAnDanh2=v,(t,v)->t.isUseAnDanh2=v),
  new Timer(10,10,ItemTime.TIME_OPEN_POWER,t->t.isOpenPower,t->t.lastTimeOpenPower,(t,v)->t.lastTimeOpenPower=v,(t,v)->t.isOpenPower=v),
  new Timer(11,11,ItemTime.TIME_MAY_DO,t->t.isUseMayDo,t->t.lastTimeUseMayDo,(t,v)->t.lastTimeUseMayDo=v,(t,v)->t.isUseMayDo=v),
  new Timer(12,-1,ItemTime.TIME_CO_BON_LA,t->t.isUseCoBonLa,t->t.lastTimeUseCoBonLa,(t,v)->t.lastTimeUseCoBonLa=v,(t,v)->t.isUseCoBonLa=v),
  new Timer(13,12,ItemTime.TIME_MAY_DO2,t->t.isUseKhoBauX2,t->t.lastTimeUseKhoBauX2,(t,v)->t.lastTimeUseKhoBauX2=v,(t,v)->t.isUseKhoBauX2=v),
  new Timer(14,25,ItemTime.TIME_BUA_SANTA,t->t.isUseBuaSanta,t->t.lastTimeBuaSanta,(t,v)->t.lastTimeBuaSanta=v,(t,v)->t.isUseBuaSanta=v),
  new Timer(15,14,ItemTime.TIME_EAT_MEAL,t->t.isEatMeal,t->t.lastTimeEatMeal,(t,v)->t.lastTimeEatMeal=v,(t,v)->t.isEatMeal=v),
  new Timer(18,17,ItemTime.TIME_CMS,t->t.isUseCMS,t->t.lastTimeUseCMS,(t,v)->t.lastTimeUseCMS=v,(t,v)->t.isUseCMS=v),
  new Timer(19,18,ItemTime.TIME_ITEM,t->t.isUseGTPT,t->t.lastTimeUseGTPT,(t,v)->t.lastTimeUseGTPT=v,(t,v)->t.isUseGTPT=v),
  new Timer(20,19,ItemTime.TIME_DK,t->t.isUseDK,t->t.lastTimeUseDK,(t,v)->t.lastTimeUseDK=v,(t,v)->t.isUseDK=v),
  new Timer(22,21,ItemTime.TIME_EAT_MEAL,t->t.isEatMeal2,t->t.lastTimeEatMeal2,(t,v)->t.lastTimeEatMeal2=v,(t,v)->t.isEatMeal2=v),
  new Timer(25,24,ItemTime.TIME_NCD,t->t.isUseNCD,t->t.lastTimeUseNCD,(t,v)->t.lastTimeUseNCD=v,(t,v)->t.isUseNCD=v),
  new Timer(26,27,ItemTime.TIME_NUOC_MIA1,t->t.isUseNuocMia1,t->t.lastTimeUseNuocMia1,(t,v)->t.lastTimeUseNuocMia1=v,(t,v)->t.isUseNuocMia1=v),
  new Timer(27,28,ItemTime.TIME_NUOC_MIA2,t->t.isUseNuocMia2,t->t.lastTimeUseNuocMia2,(t,v)->t.lastTimeUseNuocMia2=v,(t,v)->t.isUseNuocMia2=v),
  new Timer(28,29,ItemTime.TIME_NUOC_MIA3,t->t.isUseNuocMia3,t->t.lastTimeUseNuocMia3,(t,v)->t.lastTimeUseNuocMia3=v,(t,v)->t.isUseNuocMia3=v),
  new Timer(29,26,ItemTime.TIME_KILIS,t->t.isUseKilis,t->t.lastTimeUseKilis,(t,v)->t.lastTimeUseKilis=v,(t,v)->t.isUseKilis=v)
 );
 private static long value(List<?> data,int index){if(index<0||index>=data.size())return 0;Object n=data.get(index);if(n instanceof Number)return Math.max(0,((Number)n).longValue());try{return Math.max(0,Long.parseLong(String.valueOf(n)));}catch(NumberFormatException e){return 0;}}
 private static long left(boolean active,long start,long duration,long now){return active?Math.max(0,start+duration-now):0;}
 @SuppressWarnings("unchecked")
 public static JSONArray encode(ItemTime t,long now){
  JSONArray data=new JSONArray();for(int n=0;n<32;n++)data.add(0L);
  for(Timer r:TIMERS)data.set(r.index,left(r.active.test(t),r.start.applyAsLong(t),r.duration,now));
  data.set(12,left(t.isUseCoBonLa,t.lastTimeUseCoBonLa,t.timeLengthCoBonLa,now));
  data.set(29,left(t.isUseKilis,t.lastTimeUseKilis,t.timeLengthKilis>0?t.timeLengthKilis:ItemTime.TIME_KILIS,now));
  data.set(16,t.iconMeal);data.set(23,t.iconMeal2);
  data.set(17,left(t.isUseTDLT,t.lastTimeUseTDLT,t.timeTDLT,now)/60000);
  data.set(21,left(t.isUseRX,t.lastTimeUseRX,t.timeRX,now)/60000);
  data.add("item-time-v2");return data;
 }
 public static void restore(ItemTime t,List<?> data,long now){
  if(data==null)data=List.of();boolean compact=data.size()>=32;
  for(Timer r:TIMERS){long remaining=value(data,compact?r.index:r.oldIndex);long maximum=(r.index<=9||r.index==12||r.index==15||r.index==18||r.index==20||r.index==22||(r.index>=25&&r.index<=28))?SelfBuffRules.MAX_DURATION:Long.MAX_VALUE-now-r.duration;remaining=Math.min(remaining,maximum);r.setStart.accept(t,now+remaining-r.duration);r.setActive.accept(t,remaining>0);}
  t.timeLengthCoBonLa=ItemTime.TIME_CO_BON_LA;t.timeLengthKilis=ItemTime.TIME_KILIS;
  t.iconMeal=(int)value(data,compact?16:15);t.iconMeal2=(int)value(data,compact?23:22);
  t.timeTDLT=(int)Math.min(Integer.MAX_VALUE,Math.min(Integer.MAX_VALUE,value(data,compact?17:16))*60000);t.lastTimeUseTDLT=now;t.isUseTDLT=t.timeTDLT>0;
  t.timeRX=(int)Math.min(Integer.MAX_VALUE,Math.min(Integer.MAX_VALUE,value(data,compact?21:20))*60000);t.lastTimeUseRX=now;t.isUseRX=t.timeRX>0;
 }
}
