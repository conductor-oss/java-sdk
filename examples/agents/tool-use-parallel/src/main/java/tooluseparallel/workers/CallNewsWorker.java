package tooluseparallel.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Call news worker — fetches real headlines using the Wikipedia API.
 *
 * <p>For each topic in the input list, performs a Wikipedia opensearch:
 * GET https://en.wikipedia.org/w/api.php?action=opensearch&amp;search={topic}&amp;limit=5&amp;format=json
 *
 * <p>This returns the top Wikipedia article titles matching the topic, which serve as
 * real, current-event-adjacent headlines. For each result, the title and source
 * ("Wikipedia") are included.
 *
 * <p>Additionally, fetches the Portal:Current_events summary for a general overview:
 * GET https://en.wikipedia.org/api/rest_v1/page/summary/Portal:Current_events
 *
 * <p>No API key is required. Uses {@link java.net.http.HttpClient} with a 10-second timeout.
 * On network failure, returns an error map rather than crashing.
 */
public class CallNewsWorker implements Worker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getTaskDefName() {
        return "tp_call_news";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> topics = (List<String>) task.getInputData().get("topics");
        if (topics == null || topics.isEmpty()) {
            topics = List.of("general");
        }

        System.out.println("  [tp_call_news] Fetching news for topics: " + topics);

        List<Map<String, String>> headlines = new ArrayList<>();

        for (String topic : topics) {
            try {
                List<Map<String, String>> topicHeadlines = fetchHeadlinesForTopic(topic);
                headlines.addAll(topicHeadlines);
            } catch (Exception e) {
                System.out.println("  [tp_call_news] Error fetching topic '" + topic + "': " + e.getMessage());
                // Add an error entry so the caller knows this topic failed
                Map<String, String> errorEntry = new LinkedHashMap<>();
                errorEntry.put("title", "Failed to fetch headlines for: " + topic);
                errorEntry.put("topic", topic);
                errorEntry.put("source", "Wikipedia (error)");
                headlines.add(errorEntry);
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("headlines", headlines);
        result.getOutputData().put("topicsCovered", topics);
        result.getOutputData().put("source", "wikipedia");
        return result;
    }

    /**
     * Fetches up to 5 headlines for a topic using Wikipedia's opensearch API.
     * For "general" topic, also queries Portal:Current_events.
     */
    private List<Map<String, String>> fetchHeadlinesForTopic(String topic) throws Exception {
        List<Map<String, String>> headlines = new ArrayList<>();

        // If topic is "general", fetch current events summary first
        if ("general".equalsIgnoreCase(topic)) {
            try {
                Map<String, String> currentEvents = fetchCurrentEventsSummary();
                if (currentEvents != null) {
                    headlines.add(currentEvents);
                }
            } catch (Exception e) {
                // Fall through to opensearch
            }
        }

        // Use Wikipedia opensearch for the topic
        String searchTerm = "general".equalsIgnoreCase(topic) ? "current events" : topic;
        String encodedTopic = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        String url = "https://en.wikipedia.org/w/api.php?action=opensearch&search="
                + encodedTopic + "&limit=5&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ConductorWorker/1.0 (https://github.com/conductor-examples)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Wikipedia API returned status " + response.statusCode());
        }

        String body = response.body();

        // OpenSearch returns: ["query", ["title1","title2",...], ["desc1",...], ["url1",...]]
        // Parse titles from the second array
        List<String> titles = parseOpenSearchTitles(body);

        for (String title : titles) {
            Map<String, String> headline = new LinkedHashMap<>();
            headline.put("title", title);
            headline.put("topic", topic);
            headline.put("source", "Wikipedia");
            headlines.add(headline);
        }

        return headlines;
    }

    /**
     * Fetches the Portal:Current_events summary from the Wikipedia REST API.
     */
    private Map<String, String> fetchCurrentEventsSummary() throws Exception {
        String url = "https://en.wikipedia.org/api/rest_v1/page/summary/Portal:Current_events";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ConductorWorker/1.0 (https://github.com/conductor-examples)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return null;
        }

        String body = response.body();

        // Extract the "extract" field which contains the text summary
        String extract = extractJsonString(body, "\"extract\"");
        if (extract == null || extract.isBlank()) {
            return null;
        }

        // Truncate to first ~200 chars for a headline
        if (extract.length() > 200) {
            int cutoff = extract.lastIndexOf('.', 200);
            if (cutoff > 50) {
                extract = extract.substring(0, cutoff + 1);
            } else {
                extract = extract.substring(0, 200) + "...";
            }
        }

        Map<String, String> headline = new LinkedHashMap<>();
        headline.put("title", extract);
        headline.put("topic", "general");
        headline.put("source", "Wikipedia Current Events");
        return headline;
    }

    /**
     * Parses titles from a Wikipedia OpenSearch JSON response.
     * Format: ["query", ["title1","title2",...], [...], [...]]
     */
    private List<String> parseOpenSearchTitles(String json) {
        List<String> titles = new ArrayList<>();

        // Find the second array (titles array) — it starts after the first '],'
        int firstArrayEnd = json.indexOf(']');
        if (firstArrayEnd < 0) return titles;

        int secondArrayStart = json.indexOf('[', firstArrayEnd + 1);
        if (secondArrayStart < 0) return titles;
        int secondArrayEnd = json.indexOf(']', secondArrayStart);
        if (secondArrayEnd < 0) return titles;

        String arrayContent = json.substring(secondArrayStart + 1, secondArrayEnd);

        // Parse quoted strings
        int pos = 0;
        while (pos < arrayContent.length()) {
            int qStart = arrayContent.indexOf('"', pos);
            if (qStart < 0) break;
            int qEnd = findClosingQuote(arrayContent, qStart + 1);
            if (qEnd < 0) break;
            String title = arrayContent.substring(qStart + 1, qEnd);
            // Unescape basic JSON escapes
            title = title.replace("\\\"", "\"").replace("\\\\", "\\");
            titles.add(title);
            pos = qEnd + 1;
        }

        return titles;
    }

    /**
     * Finds the closing quote, handling escaped quotes.
     */
    private int findClosingQuote(String s, int from) {
        int pos = from;
        while (pos < s.length()) {
            if (s.charAt(pos) == '"' && (pos == 0 || s.charAt(pos - 1) != '\\')) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    /**
     * Extracts a string value from JSON for the given key.
     */
    private String extractJsonString(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = findClosingQuote(json, quoteStart + 1);
        if (quoteEnd < 0) return null;
        String value = json.substring(quoteStart + 1, quoteEnd);
        return value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }
}
