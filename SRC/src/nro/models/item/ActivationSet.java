package nro.models.item;

/** Set name/effect option IDs from team2026.sql item_option_template. */
public final class ActivationSet {
    private static final int[][] OPTIONS = {
        {127, 139}, {128, 140}, {129, 141}, {130, 142}, {131, 143},
        {132, 144}, {133, 136}, {134, 137}, {135, 138}, {233, 234},
        {237, 238, 239, 240}, {241, 242, 243, 244}, {245, 246, 247, 248}
    };

    private ActivationSet() {}

    public static int primaryId(int optionId) {
        for (int[] options : OPTIONS) {
            for (int id : options) {
                if (id == optionId) return options[0];
            }
        }
        return -1;
    }

    public static int[] effectIds(int primaryId) {
        for (int[] options : OPTIONS) {
            if (options[0] == primaryId) {
                return java.util.Arrays.copyOfRange(options, 1, options.length);
            }
        }
        return new int[0];
    }
}
