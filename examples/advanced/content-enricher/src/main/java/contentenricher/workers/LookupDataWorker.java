package contentenricher.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches URL metadata (title, description, word count, Open Graph tags)
 * using java.net.http.HttpClient. Falls back to defaults when no URL is provided.
 *
 * Input: customerId, enrichmentSources (map with "url" key, or a URL string)
 * Output: lookupData (map with title, description, wordCount, ogTitle, ogDescription, ogImage, customerId)
 */
public class LookupDataWorker implements Worker {

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC_PATTERN =
            Pattern.compile("<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC_PATTERN_ALT =
            Pattern.compile("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+name=[\"']description[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_TITLE_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public String getTaskDefName() {
        return "enr_lookup_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String custId = (String) task.getInputData().get("customerId");
        if (custId == null) {
            custId = "unknown";
        }

        // Try to get URL from enrichmentSources
        String url = null;
        Object sources = task.getInputData().get("enrichmentSources");
        if (sources instanceof Map) {
            Object urlObj = ((Map<String, Object>) sources).get("url");
            if (urlObj != null) url = urlObj.toString();
        } else if (sources instanceof String) {
            url = (String) sources;
        }

        System.out.println("  [lookup] Looking up enrichment data for customer \"" + custId + "\"");

        Map<String, Object> lookupData = new HashMap<>();
        lookupData.put("customerId", custId);

        if (url != null && !url.isEmpty()) {
            lookupData.putAll(fetchUrlMetadata(url));
        } else {
            lookupData.put("title", "");
            lookupData.put("description", "");
            lookupData.put("wordCount", 0);
            lookupData.put("ogTitle", "");
            lookupData.put("ogDescription", "");
            lookupData.put("ogImage", "");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("lookupData", lookupData);
        return result;
    }

    /**
     * Fetches the HTML at the given URL and extracts metadata.
     * Visible for testing.
     */
    static Map<String, Object> fetchUrlMetadata(String url) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "ConductorContentEnricher/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            metadata.put("statusCode", response.statusCode());
            metadata.put("title", extractFirst(TITLE_PATTERN, html));

            String desc = extractFirst(META_DESC_PATTERN, html);
            if (desc.isEmpty()) {
                desc = extractFirst(META_DESC_PATTERN_ALT, html);
            }
            metadata.put("description", desc);

            // Word count: strip scripts, styles, and HTML tags, then count tokens
            String textOnly = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ")
                    .replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&[a-zA-Z]+;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            int wordCount = textOnly.isEmpty() ? 0 : textOnly.split("\\s+").length;
            metadata.put("wordCount", wordCount);

            metadata.put("ogTitle", extractFirst(OG_TITLE_PATTERN, html));
            metadata.put("ogDescription", extractFirst(OG_DESC_PATTERN, html));
            metadata.put("ogImage", extractFirst(OG_IMAGE_PATTERN, html));

            System.out.println("  [lookup] Fetched metadata: title=\"" + metadata.get("title")
                    + "\", wordCount=" + wordCount);

        } catch (Exception e) {
            System.out.println("  [lookup] Error fetching URL " + url + ": " + e.getMessage());
            metadata.put("title", "");
            metadata.put("description", "");
            metadata.put("wordCount", 0);
            metadata.put("ogTitle", "");
            metadata.put("ogDescription", "");
            metadata.put("ogImage", "");
            metadata.put("error", e.getMessage());
        }
        return metadata;
    }

    /**
     * Parses HTML from a string directly (used for unit testing without HTTP).
     * Visible for testing.
     */
    static Map<String, Object> parseHtmlMetadata(String html) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", extractFirst(TITLE_PATTERN, html));

        String desc = extractFirst(META_DESC_PATTERN, html);
        if (desc.isEmpty()) {
            desc = extractFirst(META_DESC_PATTERN_ALT, html);
        }
        metadata.put("description", desc);

        String textOnly = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ")
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&[a-zA-Z]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        int wordCount = textOnly.isEmpty() ? 0 : textOnly.split("\\s+").length;
        metadata.put("wordCount", wordCount);

        metadata.put("ogTitle", extractFirst(OG_TITLE_PATTERN, html));
        metadata.put("ogDescription", extractFirst(OG_DESC_PATTERN, html));
        metadata.put("ogImage", extractFirst(OG_IMAGE_PATTERN, html));
        return metadata;
    }

    private static String extractFirst(Pattern pattern, String html) {
        Matcher m = pattern.matcher(html);
        return m.find() ? m.group(1).trim() : "";
    }
}
