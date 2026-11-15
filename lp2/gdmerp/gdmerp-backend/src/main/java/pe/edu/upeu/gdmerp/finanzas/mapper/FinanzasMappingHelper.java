package pe.edu.upeu.gdmerp.finanzas.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FinanzasMappingHelper {

    private FinanzasMappingHelper() {
    }

    public static Map<Long, Integer> toCountMap(List<Object[]> rows) {
        Map<Long, Integer> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            Number total = (Number) row[1];
            counts.put(id, total != null ? total.intValue() : 0);
        }
        return counts;
    }
}
