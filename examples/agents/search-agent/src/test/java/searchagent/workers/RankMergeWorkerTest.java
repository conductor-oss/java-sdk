package searchagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RankMergeWorkerTest {

    private final RankMergeWorker worker = new RankMergeWorker();

    @Test
    void taskDefName() {
        assertEquals("sa_rank_merge", worker.getTaskDefName());
    }

    @Test
    void mergesGoogleAndWikiResults() {
        List<Map<String, Object>> googleResults = List.of(
                Map.of("title", "Google Result 1", "relevance", 0.9, "source", "google"),
                Map.of("title", "Google Result 2", "relevance", 0.7, "source", "google"));
        List<Map<String, Object>> wikiResults = List.of(
                Map.of("title", "Wiki Result 1", "relevance", 0.85, "source", "wikipedia"));

        Task task = taskWith(Map.of(
                "question", "test query",
                "googleResults", googleResults,
                "wikiResults", wikiResults));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("totalResults"));
    }

    @Test
    void sortsResultsByRelevanceDescending() {
        List<Map<String, Object>> googleResults = List.of(
                Map.of("title", "Low", "relevance", 0.5, "source", "google"));
        List<Map<String, Object>> wikiResults = List.of(
                Map.of("title", "High", "relevance", 0.95, "source", "wikipedia"),
                Map.of("title", "Mid", "relevance", 0.75, "source", "wikipedia"));

        Task task = taskWith(Map.of(
                "question", "test",
                "googleResults", googleResults,
                "wikiResults", wikiResults));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranked =
                (List<Map<String, Object>>) result.getOutputData().get("rankedResults");
        assertEquals("High", ranked.get(0).get("title"));
        assertEquals("Mid", ranked.get(1).get("title"));
        assertEquals("Low", ranked.get(2).get("title"));
    }

    @Test
    void returnsTopThreeSources() {
        List<Map<String, Object>> googleResults = List.of(
                Map.of("title", "A", "relevance", 0.9, "source", "google"),
                Map.of("title", "B", "relevance", 0.8, "source", "google"));
        List<Map<String, Object>> wikiResults = List.of(
                Map.of("title", "C", "relevance", 0.85, "source", "wikipedia"),
                Map.of("title", "D", "relevance", 0.6, "source", "wikipedia"));

        Task task = taskWith(Map.of(
                "question", "test",
                "googleResults", googleResults,
                "wikiResults", wikiResults));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> topSources = (List<String>) result.getOutputData().get("topSources");
        assertEquals(3, topSources.size());
        // Top 3 by relevance: A(0.9), C(0.85), B(0.8)
        assertEquals("A", topSources.get(0));
        assertEquals("C", topSources.get(1));
        assertEquals("B", topSources.get(2));
    }

    @Test
    void handlesNullGoogleResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "test");
        input.put("googleResults", null);
        input.put("wikiResults", List.of(
                Map.of("title", "W1", "relevance", 0.8, "source", "wikipedia")));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalResults"));
    }

    @Test
    void handlesNullWikiResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "test");
        input.put("googleResults", List.of(
                Map.of("title", "G1", "relevance", 0.9, "source", "google")));
        input.put("wikiResults", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalResults"));
    }

    @Test
    void handlesBothResultsEmpty() {
        Task task = taskWith(Map.of(
                "question", "test",
                "googleResults", List.of(),
                "wikiResults", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalResults"));

        @SuppressWarnings("unchecked")
        List<String> topSources = (List<String>) result.getOutputData().get("topSources");
        assertTrue(topSources.isEmpty());
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("googleResults", List.of());
        input.put("wikiResults", List.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
