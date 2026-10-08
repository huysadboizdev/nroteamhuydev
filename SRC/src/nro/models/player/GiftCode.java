package nro.models.player;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author By Mr Blue
 * 
 */

public class GiftCode {

    public List<String> rewards;

    public GiftCode() {
        this.rewards = new ArrayList<>();
    }

    public void add(String code) {
        String canonical = canonical(code);
        if (!isUsedGiftCode(canonical)) this.rewards.add(canonical);
    }

    public boolean isUsedGiftCode(String code) {
        String canonical = canonical(code);
        for (String prior : rewards) if (canonical(prior).equals(canonical)) return true;
        return false;
    }

    /** Match PHP and migration exactly: ASCII whitespace trim, ASCII uppercase. */
    public static String canonical(String raw) {
        if (raw == null) throw new IllegalArgumentException("Missing giftcode");
        int first=0,last=raw.length();
        while(first<last && whitespace(raw.charAt(first)))first++;
        while(last>first && whitespace(raw.charAt(last-1)))last--;
        String value=raw.substring(first,last);
        if(value.isEmpty()||value.length()>100)throw new IllegalArgumentException("Giftcode must contain 1..100 characters");
        StringBuilder out=new StringBuilder();
        for(char ch:value.toCharArray()) {
            if(ch<33||ch>126)throw new IllegalArgumentException("Giftcode must use printable ASCII without spaces");
            out.append(ch>='a'&&ch<='z'?(char)(ch-32):ch);
        }
        return out.toString();
    }
    private static boolean whitespace(char ch){return ch==' '||ch=='\t'||ch=='\n'||ch=='\r'||ch=='\u000b'||ch=='\f';}

    public void dispose() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }
    }

}
