package nro.models.data;

import java.io.IOException;
import java.util.*;
import java.util.function.ToIntFunction;

/** Protocol templates have implicit IDs: preserve gaps instead of shifting rows. */
public final class TemplateIndex {
    private TemplateIndex() { }

    public static <T> List<T> byId(Collection<T> rows, ToIntFunction<T> id) throws IOException {
        List<T> slots = new ArrayList<>();
        for (T row : rows) {
            int index = id.applyAsInt(row);
            if (index < 0 || index >= 255) throw new IOException("Template ID outside byte count: " + index);
            while (slots.size() <= index) slots.add(null);
            if (slots.get(index) != null) throw new IOException("Duplicate template ID " + index);
            slots.set(index, row);
        }
        return slots;
    }
}
