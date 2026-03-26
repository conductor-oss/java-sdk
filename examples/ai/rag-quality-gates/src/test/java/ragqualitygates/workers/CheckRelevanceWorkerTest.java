package ragqualitygates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckRelevanceWorkerTest {

    private final CheckRelevanceWorker worker = new CheckRelevanceWorker();

    @Test
    void taskDefName() {
        assertEquals("qg_check_relevance", worker.getTaskDefName());
    }

    @Test
    void passesWhenAverageAboveThreshold() {
        List<Map<String, Object>> documents = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Doc 1", "score", 0.92)),
                new HashMap<>(Map.of("id", "doc-2", "text", "Doc 2", "score", 0.78)),
                new HashMap<>(Map.of("id", "doc-3", "text", "Doc 3", "score", 0.55))
        );

        Task task = taskWith(new HashMap<>(Map.of("documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("pass", result.getOutputData().get("decision"));
        assertEquals(0.7, result.getOutputData().get("threshold"));

        double relevanceScore = (double) result.getOutputData().get("relevanceScore");
        assertTrue(relevanceScore >= 0.7);
        assertEquals(0.75, relevanceScore, 0.01);
    }

    @Test
    void failsWhenAverageBelowThreshold() {
        List<Map<String, Object>> documents = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Doc 1", "score", 0.5)),
                new HashMap<>(Map.of("id", "doc-2", "text", "Doc 2", "score", 0.4)),
                new HashMap<>(Map.of("id", "doc-3", "text", "Doc 3", "score", 0.3))
        );

        Task task = taskWith(new HashMap<>(Map.of("documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fail", result.getOutputData().get("decision"));

        double relevanceScore = (double) result.getOutputData().get("relevanceScore");
        assertTrue(relevanceScore < 0.7);
    }

    @Test
    void handlesNullDocuments() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fail", result.getOutputData().get("decision"));
        assertEquals(0.0, result.getOutputData().get("relevanceScore"));
    }

    @Test
    void passesAtExactThreshold() {
        List<Map<String, Object>> documents = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Doc 1", "score", 0.7))
        );

        Task task = taskWith(new HashMap<>(Map.of("documents", documents)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("pass", result.getOutputData().get("decision"));
        assertEquals(0.7, (double) result.getOutputData().get("relevanceScore"), 0.001);
    }

    @Test
    void returnsThresholdValue() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(0.7, result.getOutputData().get("threshold"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
