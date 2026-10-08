package nro.models.player;

/** Saturating positive EXP arithmetic, preserving long through combat bonuses. */
public final class ExpArithmetic {
    private ExpArithmetic() {}
    public static long multiply(long value,long multiplier) {
        if(value<=0||multiplier<=0)return 0;
        return value>Long.MAX_VALUE/multiplier?Long.MAX_VALUE:value*multiplier;
    }
    public static long add(long value,long increase) {
        return value>Long.MAX_VALUE-increase?Long.MAX_VALUE:value+increase;
    }
    public static long percent(long value,long percent) {
        return add(multiply(value/100,percent),multiply(value%100,percent)/100);
    }
}
