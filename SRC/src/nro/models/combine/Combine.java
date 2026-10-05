package nro.models.combine;
import nro.models.item.Item;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author By Mr Blue
 */

public class Combine {

    public long lastTimeCombine;

    public List<Item> itemsCombine;
    public int typeCombine;
    public NangSKHVip.Selection skhVipSelection;
    public NangSKHThuong.Selection skhThuongSelection;
    public ChuyenHoaTrangBiRules.Selection chuyenHoaSelection;

    public void setTypeCombine(int type) {
        clearItemCombine();
        clearParamCombine();
        this.typeCombine = type;
    }

    public int goldCombine;
    public int gemCombine;
    public float ratioCombine;
    public int countDaNangCap;
    public short countDaBaoVe;

    public Combine() {
        this.itemsCombine = new ArrayList<>();
    }

    public void clearItemCombine() {
        this.chuyenHoaSelection = null;
        this.skhVipSelection = null;
        this.skhThuongSelection = null;
        this.itemsCombine.clear();
    }

    public void clearParamCombine() {
        this.goldCombine = 0;
        this.gemCombine = 0;
        this.ratioCombine = 0;
        this.countDaNangCap = 0;
        this.countDaBaoVe = 0;

    }

    public void dispose() {
        this.chuyenHoaSelection = null;
        this.skhVipSelection = null;
        this.skhThuongSelection = null;
        this.itemsCombine = null;
    }
}
