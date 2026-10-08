package nro.models.services;

public record QuestSupportConfig(boolean enabled, int totalChains, int restSeconds, int graceSeconds) {
    public static final QuestSupportConfig DEFAULT = new QuestSupportConfig(true, 3, 120, 300);
    public QuestSupportConfig {
        if (totalChains < 1 || totalChains > 3 || restSeconds < 1 || restSeconds > 3600
                || graceSeconds < 0 || graceSeconds > 300) {
            throw new IllegalArgumentException("Invalid quest support limits");
        }
    }
}
