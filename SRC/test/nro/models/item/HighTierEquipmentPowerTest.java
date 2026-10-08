package nro.models.item;

import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.PinkFixture;
import nro.models.services.Service;

public final class HighTierEquipmentPowerTest {
    private static int power(int id) {
        if (id >= 1048) return 80;
        return switch (id) {
            case 650,652,654 -> 40;
            case 651,653,655 -> 44;
            case 657,659,661 -> 50;
            case 658,660,662 -> 43;
            case 656 -> 47;
            default -> throw new AssertionError("unexpected id");
        };
    }
    private static int slot(int id) {
        if(id>=1048)return (id-1048)/3;
        return switch(id) {
            case 650,652,654 -> 0;
            case 651,653,655 -> 1;
            case 657,659,661 -> 2;
            case 658,660,662 -> 3;
            case 656 -> 4;
            default -> throw new AssertionError("unexpected id");
        };
    }
    private static int gender(int id) {
        if(id>=1048)return (id-1048)%3;
        return switch(id) {
            case 650,651,657,658 -> 0;
            case 652,653,659,660 -> 1;
            case 654,655,661,662 -> 2;
            case 656 -> 3;
            default -> throw new AssertionError("unexpected id");
        };
    }
    public static void main(String[] args) throws Exception {
        PinkFixture.install();
        PinkFixture.replace(Service.class,"instance",new Service(){
            @Override public void sendThongBaoOK(Player p,String message) {}
            @Override public void point(Player p) {}
            @Override public void Send_Caitrang(Player p) {}
        });
        int tested=0;
        for(int id : java.util.stream.IntStream.concat(java.util.stream.IntStream.rangeClosed(650,662),
                java.util.stream.IntStream.rangeClosed(1048,1062)).toArray()) {
            Item item=PinkFixture.item(id,1);
            item.template.type=(byte)slot(id);item.template.gender=(byte)gender(id);
            item.itemOptions.add(new Item.ItemOption(21,1));
            item.itemOptions.add(new Item.ItemOption(21,150));
            item.itemOptions.add(new Item.ItemOption(30,1));
            item.itemOptions.add(new Item.ItemOption(77,5));
            ItemDefaultOptions.complete(item);ItemDefaultOptions.complete(item);
            PinkFixture.check(item.getOptionParam(21)==power(id),"canonical required power item "+id);
            PinkFixture.check(item.itemOptions.stream().filter(o->o.optionTemplate.id==21).count()==1,"one power option");
            PinkFixture.check(item.itemOptions.stream().filter(o->o.optionTemplate.id==30).count()==1
                    &&item.getOptionParam(30)==0,"one lock option");
            PinkFixture.check(item.getOptionParam(77)==5,"bonus preserved");
            // Forge the saved option again: authoritative equip check must ignore it.
            item.itemOptions.removeIf(o->o.optionTemplate.id==21);
            item.itemOptions.add(new Item.ItemOption(21,1));
            Player player=new Player();player.gender=(byte)(gender(id)==3?0:gender(id));
            player.inventory.itemsBody.clear();
            for(int index=0;index<5;index++)player.inventory.itemsBody.add(new Item());
            player.nPoint.power=(long)power(id)*1_000_000_000L-1;
            InventoryService inventory=new InventoryService();
            PinkFixture.check(inventory.putItemBody(player,item)==item,"below threshold rejected "+id);
            PinkFixture.check(player.inventory.itemsBody.stream().noneMatch(Item::isNotNullItem),"body unchanged");
            player.nPoint.power++;
            inventory.putItemBody(player,item);
            PinkFixture.check(player.inventory.itemsBody.get(slot(id))==item,"threshold accepted in correct slot "+id);
            player.inventory.itemsBody.set(slot(id),new Item());
            player.inventory.itemsBox.clear();player.inventory.itemsBox.add(item);
            InventoryService boxInventory=new InventoryService(){
                @Override public void sendItemBody(Player p) {}
                @Override public boolean addItemBag(Player p,Item i) {return false;}
            };
            player.nPoint.power--;
            boxInventory.itemBoxToBodyOrBag(player,0);
            PinkFixture.check(!player.inventory.itemsBody.get(slot(id)).isNotNullItem()
                    &&player.inventory.itemsBox.get(0)==item,"box route below threshold rejected "+id);
            player.nPoint.power++;
            boxInventory.itemBoxToBodyOrBag(player,0);
            PinkFixture.check(player.inventory.itemsBody.get(slot(id))==item
                    &&!player.inventory.itemsBox.get(0).isNotNullItem(),"box route exact threshold equipped "+id);
            tested++;
        }
        Item ordinary=PinkFixture.item(555,1);ordinary.itemOptions.add(new Item.ItemOption(21,10));
        ItemDefaultOptions.complete(ordinary);
        PinkFixture.check(ordinary.getOptionParam(21)==10,"ordinary divine requirement preserved");
        Item boosted=PinkFixture.item(1048,1);boosted.template.type=0;boosted.template.isUpToUp=false;
        boosted.itemOptions.add(new Item.ItemOption(21,160));
        boosted.itemOptions.add(new Item.ItemOption(30,2));
        boosted.itemOptions.add(new Item.ItemOption(47,6400));
        boosted.itemOptions.add(new Item.ItemOption(93,7));
        boosted.createTime=System.currentTimeMillis()-1234;
        long timestamp=boosted.createTime;
        Player recipient=new Player();recipient.inventory.itemsBag.clear();recipient.inventory.itemsBag.add(new Item());
        PinkFixture.check(new InventoryService().addItemBag(recipient,boosted),"boosted crafted angel granted");
        Item granted=recipient.inventory.itemsBag.get(0);
        PinkFixture.check(granted.getOptionParam(21)==80&&granted.getOptionParam(30)==0,"grant canonicalizes multiplied power and lock");
        PinkFixture.check(granted.getOptionParam(47)==6400&&granted.getOptionParam(93)==7&&granted.createTime==timestamp,
                "grant preserves crafted stats and lifetime");
        System.out.println("HighTierEquipmentPowerTest PASS: "+tested+" high-tier items, ordinary control and real crafted grant");
        System.exit(0);
    }
}
