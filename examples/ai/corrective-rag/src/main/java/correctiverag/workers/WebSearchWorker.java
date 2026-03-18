package correctiverag.workers;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a web search fallback when retrieved documents are irrelevant.
 *
 * <p>Fetches real web pages using java.net.http.HttpClient and extracts text
 * snippets from the HTML. Searches Wikipedia for factual content. Returns up to
 * 3 web results with title and snippet.
 */
public class WebSearchWorker implements Worker {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_RESULTS = 3;

    private final HttpClient httpClient;

    public WebSearchWorker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getTaskDefName() {
        return "cr_web_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "Conductor workflow orchestration";
        }

        System.out.println("  [web] Falling back to web search: \"" + question + "\"");

        List<Map<String, String>> webResults = new ArrayList<>();

        try {
            // Use Wikipedia's API to search for relevant articles
            String encoded = URLEncoder.encode(question, StandardCharsets.UTF_8);
            String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search"
                    + "&srsearch=" + encoded
                    + "&srlimit=" + MAX_RESULTS
                    + "&format=json&utf8=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("User-Agent", "ConductorExample/1.0 (conductor-examples; educational)")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            System.out.println("  [web] Querying Wikipedia search API...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                webResults = parseWikipediaSearchResults(response.body());
                System.out.println("  [web] Retrieved " + webResults.size() + " web results");
            } else {
                System.err.println("  [web] Wikipedia API returned HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("  [web] HTTP request failed: " + e.getMessage());
        }

        // If the HTTP request failed or returned no results, return an empty list
        // rather than fake data — the downstream worker should handle empty results
        if (webResults.isEmpty()) {
            System.out.println("  [web] No web results found");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("webResults", webResults);
        return result;
    }

    /**
     * Parses Wikipedia API search results JSON (minimal parsing without a JSON library).
     * Expected format: {"query":{"search":[{"title":"...","snippet":"..."},...]}}
     */
    private List<Map<String, String>> parseWikipediaSearchResults(String json) {
        List<Map<String, String>> results = new ArrayList<>();

        // Find the "search" array
        int searchIdx = json.indexOf("\"search\"");
        if (searchIdx < 0) return results;

        // Extract individual result objects
        Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
        Pattern snippetPattern = Pattern.compile("\"snippet\"\\s*:\\s*\"([^\"]+)\"");

        // Split by result objects (each starts with {"ns":)
        String searchSection = json.substring(searchIdx);
        String[] parts = searchSection.split("\\{\"ns\"");

        for (int i = 1; i < parts.length && results.size() < MAX_RESULTS; i++) {
            String part = parts[i];
            Matcher titleMatcher = titlePattern.matcher(part);
            Matcher snippetMatcher = snippetPattern.matcher(part);

            if (titleMatcher.find() && snippetMatcher.find()) {
                String title = titleMatcher.group(1);
                String snippet = snippetMatcher.group(1)
                        .replaceAll("<[^>]+>", "")  // strip HTML tags
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&amp;", "&")
                        .replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">");

                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("title", title);
                entry.put("snippet", snippet);
                results.add(entry);
            }
        }

        return results;
    }
}
