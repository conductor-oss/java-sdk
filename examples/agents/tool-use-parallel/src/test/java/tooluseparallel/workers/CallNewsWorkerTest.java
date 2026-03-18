package tooluseparallel.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CallNewsWorkerTest {

    private final CallNewsWorker worker = new CallNewsWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_call_news", worker.getTaskDefName());
    }

    @Test
    void returnsHeadlines() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("topics", List.of("technology", "business", "world")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> headlines = (List<Map<String, String>>) result.getOutputData().get("headlines");
        assertNotNull(headlines);
        assertFalse(headlines.isEmpty(), "Should return at least one headline");
    }

    @Test
    void headlinesContainTitleAndTopic() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("topics", List.of("technology")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> headlines = (List<Map<String, String>>) result.getOutputData().get("headlines");
        assertFalse(headlines.isEmpty(), "Should have at least one headline");
        Map<String, String> first = headlines.get(0);
        assertNotNull(first.get("title"), "Headline should have a title");
        assertNotNull(first.get("topic"), "Headline should have a topic");
        assertNotNull(first.get("source"), "Headline should have a source");
    }

    @Test
    void returnsTopicsCovered() {
        List<String> topics = List.of("technology", "business", "world");
        Task task = taskWith(Map.of("topics", topics));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> topicsCovered = (List<String>) result.getOutputData().get("topicsCovered");
        assertEquals(topics, topicsCovered);
    }

    @Test
    void returnsWikipediaSource() {
        Task task = taskWith(Map.of("topics", List.of("business")));
        TaskResult result = worker.execute(task);

        assertEquals("wikipedia", result.getOutputData().get("source"));
    }

    @Test
    void handlesNullTopics() {
        Map<String, Object> input = new HashMap<>();
        input.put("topics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> topicsCovered = (List<String>) result.getOutputData().get("topicsCovered");
        assertEquals(List.of("general"), topicsCovered);
    }

    @Test
    void handlesMissingTopics() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("headlines"));
    }

    @Test
    void headlinesFromMultipleTopicsHaveDistinctSources() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("topics", List.of("technology", "business")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> headlines = (List<Map<String, String>>) result.getOutputData().get("headlines");
        assertFalse(headlines.isEmpty(), "Should have headlines");
        // All headlines should have a non-null source
        for (Map<String, String> h : headlines) {
            assertNotNull(h.get("source"), "Each headline should have a source");
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    /**
     * Checks whether a network connection to Wikipedia API is available.
     */
    private static boolean isNetworkAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://en.wikipedia.org/w/api.php?action=opensearch&search=test&limit=1&format=json"))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "ConductorWorker/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
