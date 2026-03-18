package mapreduce.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility for map workers: counts case-insensitive occurrences
 * of a search term in text documents within a partition.
 */
final class MapWorkerUtil {

    private MapWorkerUtil() {}

    /**
     * Counts occurrences of searchTerm in each document in the partition.
     *
     * @param partObj    the partition (expected List of Strings)
     * @param searchTerm the term to search for (case-insensitive)
     * @param label      label for logging
     * @return list of maps, each with "docIndex" (int) and "count" (int)
     */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> countInPartition(Object partObj, String searchTerm, String label) {
        List<String> docs = new ArrayList<>();
        if (partObj instanceof List) {
            for (Object item : (List<?>) partObj) {
                docs.add(item != null ? item.toString() : "");
            }
        }

        if (searchTerm == null) searchTerm = "";

        List<Map<String, Object>> mapped = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            int count = countOccurrences(docs.get(i), searchTerm);
            Map<String, Object> entry = new HashMap<>();
            entry.put("docIndex", i);
            entry.put("count", count);
            entry.put("text", docs.get(i).length() > 80
                    ? docs.get(i).substring(0, 80) + "..."
                    : docs.get(i));
            mapped.add(entry);
        }
        return mapped;
    }

    /**
     * Counts non-overlapping, case-insensitive occurrences of term in text.
     * If term is empty, returns the word count of the text.
     */
    static int countOccurrences(String text, String term) {
        if (text == null || text.isEmpty()) return 0;
        if (term == null || term.isEmpty()) {
            // If no search term, return word count
            return text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        }
        String lowerText = text.toLowerCase();
        String lowerTerm = term.toLowerCase();
        int count = 0;
        int idx = 0;
        while ((idx = lowerText.indexOf(lowerTerm, idx)) != -1) {
            count++;
            idx += lowerTerm.length();
        }
        return count;
    }
}
