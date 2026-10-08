package nro.models.event;
import java.util.*;
import nro.models.player.*;
import nro.models.services_func.*;
import static nro.models.services.PinkFixture.*;
/** Closing a theme cancels its staged trades before bag cleanup. */
public final class EventTradeCutoffTest {
 public static void main(String[] args)throws Exception {
  install();Player a=player(901),b=player(902);a.idMark=new IDMark();b.idMark=new IDMark();
  EventItemPolicy.publish(Map.of("summer",true));a.inventory.itemsBag.add(item(1612,5));
  Trade trade=new Trade(a,b);TransactionService.cancelDisabledEventTrades();
  check(TransactionService.isTrading(a),"enabled event trade remains open");
  EventItemPolicy.publish(Map.of("summer",false));TransactionService.cancelDisabledEventTrades();
  check(!TransactionService.isTrading(a)&&!TransactionService.isTrading(b),"both staged trade identities removed before cleanup");
  check(a.inventory.itemsBag.get(0).quantity==5&&b.inventory.itemsBag.isEmpty(),"cancel does not debit fees or publish staged bags");
  a.inventory.itemsBag.clear();a.inventory.itemsBag.add(item(1804,1));
  trade=new Trade(a,b);TransactionService.cancelDisabledEventTrades();
  check(TransactionService.isTrading(a),"permanent gift remains tradable after theme closes");trade.dispose();
  System.out.println("EventTradeCutoffTest PASS");System.exit(0);
 }
}
