package ragmultiquery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DedupResultsWorkerTest {

    private final DedupResultsWorker worker = new DedupResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("mq_dedup_results", worker.getTaskDefName());
    }

    @Test
    void deduplicatesOverlappingResults() {
        List<Map<String, String>> results1 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d1", "text", "Workflow orchestration provides centralized control and visibility.")),
                new HashMap<>(Map.of("id", "d4", "text", "Orchestration enables retry, timeout, and compensation logic."))
        ));
        List<Map<String, String>> results2 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d1", "text", "Workflow orchestration provides centralized control and visibility.")),
                new HashMap<>(Map.of("id", "d7", "text", "Conductor decouples task execution from workflow logic.")),
                new HashMap<>(Map.of("id", "d9", "text", "Conductor supports versioned workflows for safe deployments."))
        ));
        List<Map<String, String>> results3 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d4", "text", "Orchestration enables retry, timeout, and compensation logic.")),
                new HashMap<>(Map.of("id", "d11", "text", "Choreography can lead to hidden dependencies and hard-to-debug flows."))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "results1", results1,
                "results2", results2,
                "results3", results3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(7, result.getOutputData().get("totalRetrieved"));
        assertEquals(5, result.getOutputData().get("uniqueCount"));

        @SuppressWarnings("unchecked")
        List<String> uniqueDocs = (List<String>) result.getOutputData().get("uniqueDocs");
        assertEquals(5, uniqueDocs.size());
    }

    @Test
    void preservesOrderOfFirstOccurrence() {
        List<Map<String, String>> results1 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d1", "text", "First")),
                new HashMap<>(Map.of("id", "d4", "text", "Second"))
        ));
        List<Map<String, String>> results2 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d1", "text", "First duplicate")),
                new HashMap<>(Map.of("id", "d7", "text", "Third"))
        ));
        List<Map<String, String>> results3 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "d4", "text", "Second duplicate"))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "results1", results1,
                "results2", results2,
                "results3", results3
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> uniqueDocs = (List<String>) result.getOutputData().get("uniqueDocs");
        assertEquals("First", uniqueDocs.get(0));
        assertEquals("Second", uniqueDocs.get(1));
        assertEquals("Third", uniqueDocs.get(2));
    }

    @Test
    void handlesNullResults() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalRetrieved"));
        assertEquals(0, result.getOutputData().get("uniqueCount"));
    }

    @Test
    void handlesNoOverlaps() {
        List<Map<String, String>> results1 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "a", "text", "A"))
        ));
        List<Map<String, String>> results2 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "b", "text", "B"))
        ));
        List<Map<String, String>> results3 = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "c", "text", "C"))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "results1", results1,
                "results2", results2,
                "results3", results3
        )));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalRetrieved"));
        assertEquals(3, result.getOutputData().get("uniqueCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
