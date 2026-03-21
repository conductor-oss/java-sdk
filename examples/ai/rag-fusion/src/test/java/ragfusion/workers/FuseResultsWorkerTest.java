package ragfusion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FuseResultsWorkerTest {

    private final FuseResultsWorker worker = new FuseResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("rf_fuse_results", worker.getTaskDefName());
    }

    @Test
    void fusesResultsFromThreeSources() {
        List<Map<String, Object>> r1 = List.of(
                new HashMap<>(Map.of("id", "doc-a", "text", "Text A", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-b", "text", "Text B", "rank", 2))
        );
        List<Map<String, Object>> r2 = List.of(
                new HashMap<>(Map.of("id", "doc-b", "text", "Text B", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-c", "text", "Text C", "rank", 2))
        );
        List<Map<String, Object>> r3 = List.of(
                new HashMap<>(Map.of("id", "doc-a", "text", "Text A", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-c", "text", "Text C", "rank", 2))
        );

        Task task = taskWith(new HashMap<>(Map.of("results1", r1, "results2", r2, "results3", r3)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fusedDocs = (List<Map<String, Object>>) result.getOutputData().get("fusedDocs");
        assertNotNull(fusedDocs);
        assertEquals(3, fusedDocs.size());
        assertEquals(3, result.getOutputData().get("fusedCount"));
        assertEquals(6, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void rrfScoresAreCorrectWithK60() {
        // doc-a: rank 1 in r1 + rank 1 in r3 => 1/61 + 1/61 = 2/61
        // doc-b: rank 2 in r1 + rank 1 in r2 => 1/62 + 1/61
        // doc-c: rank 2 in r2 + rank 2 in r3 => 1/62 + 1/62 = 2/62
        List<Map<String, Object>> r1 = List.of(
                new HashMap<>(Map.of("id", "doc-a", "text", "A", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-b", "text", "B", "rank", 2))
        );
        List<Map<String, Object>> r2 = List.of(
                new HashMap<>(Map.of("id", "doc-b", "text", "B", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-c", "text", "C", "rank", 2))
        );
        List<Map<String, Object>> r3 = List.of(
                new HashMap<>(Map.of("id", "doc-a", "text", "A", "rank", 1)),
                new HashMap<>(Map.of("id", "doc-c", "text", "C", "rank", 2))
        );

        Task task = taskWith(new HashMap<>(Map.of("results1", r1, "results2", r2, "results3", r3)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fusedDocs = (List<Map<String, Object>>) result.getOutputData().get("fusedDocs");

        // doc-a should be first (highest RRF score: 2/61)
        assertEquals("doc-a", fusedDocs.get(0).get("id"));
        // doc-b should be second (1/62 + 1/61)
        assertEquals("doc-b", fusedDocs.get(1).get("id"));
        // doc-c should be last (2/62)
        assertEquals("doc-c", fusedDocs.get(2).get("id"));

        // Verify scores are positive numbers
        for (Map<String, Object> doc : fusedDocs) {
            double score = ((Number) doc.get("rrfScore")).doubleValue();
            assertTrue(score > 0);
        }
    }

    @Test
    void sortedByRrfScoreDescending() {
        List<Map<String, Object>> r1 = List.of(
                new HashMap<>(Map.of("id", "x", "text", "X", "rank", 3))
        );
        List<Map<String, Object>> r2 = List.of(
                new HashMap<>(Map.of("id", "y", "text", "Y", "rank", 1))
        );
        List<Map<String, Object>> r3 = List.of(
                new HashMap<>(Map.of("id", "y", "text", "Y", "rank", 1)),
                new HashMap<>(Map.of("id", "x", "text", "X", "rank", 2))
        );

        Task task = taskWith(new HashMap<>(Map.of("results1", r1, "results2", r2, "results3", r3)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fusedDocs = (List<Map<String, Object>>) result.getOutputData().get("fusedDocs");

        // y appears at rank 1 twice => 2/61, x appears at rank 3 once and rank 2 once => 1/63 + 1/62
        assertEquals("y", fusedDocs.get(0).get("id"));
        assertEquals("x", fusedDocs.get(1).get("id"));

        double score0 = ((Number) fusedDocs.get(0).get("rrfScore")).doubleValue();
        double score1 = ((Number) fusedDocs.get(1).get("rrfScore")).doubleValue();
        assertTrue(score0 > score1);
    }

    @Test
    void deduplicatesAcrossLists() {
        List<Map<String, Object>> r1 = List.of(
                new HashMap<>(Map.of("id", "same", "text", "Same doc", "rank", 1))
        );
        List<Map<String, Object>> r2 = List.of(
                new HashMap<>(Map.of("id", "same", "text", "Same doc", "rank", 1))
        );
        List<Map<String, Object>> r3 = List.of(
                new HashMap<>(Map.of("id", "same", "text", "Same doc", "rank", 1))
        );

        Task task = taskWith(new HashMap<>(Map.of("results1", r1, "results2", r2, "results3", r3)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fusedDocs = (List<Map<String, Object>>) result.getOutputData().get("fusedDocs");

        assertEquals(1, fusedDocs.size());
        assertEquals(1, result.getOutputData().get("fusedCount"));
        assertEquals(3, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesNullResults() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fusedDocs = (List<Map<String, Object>>) result.getOutputData().get("fusedDocs");
        assertNotNull(fusedDocs);
        assertEquals(0, fusedDocs.size());
        assertEquals(0, result.getOutputData().get("fusedCount"));
        assertEquals(0, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void handlesPartialNullResults() {
        List<Map<String, Object>> r1 = List.of(
                new HashMap<>(Map.of("id", "doc-x", "text", "X", "rank", 1))
        );

        HashMap<String, Object> input = new HashMap<>();
        input.put("results1", r1);
        input.put("results2", null);
        input.put("results3", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("fusedCount"));
        assertEquals(1, result.getOutputData().get("totalCandidates"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
