package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradeDocsWorkerTest {

    private final GradeDocsWorker worker = new GradeDocsWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_grade_docs", worker.getTaskDefName());
    }

    @Test
    void filtersDocsWithScoreBelow05() {
        List<Map<String, Object>> docs = List.of(
                Map.of("id", "d1", "text", "relevant", "score", 0.91),
                Map.of("id", "d2", "text", "relevant", "score", 0.85),
                Map.of("id", "d3", "text", "relevant", "score", 0.78),
                Map.of("id", "d4", "text", "irrelevant", "score", 0.22)
        );

        Task task = taskWith(new HashMap<>(Map.of("question", "test", "documents", docs)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relevant =
                (List<Map<String, Object>>) result.getOutputData().get("relevantDocs");
        assertEquals(3, relevant.size());
        assertEquals(1, result.getOutputData().get("filteredCount"));
    }

    @Test
    void allDocsRelevant() {
        List<Map<String, Object>> docs = List.of(
                Map.of("id", "d1", "score", 0.9),
                Map.of("id", "d2", "score", 0.5)
        );

        Task task = taskWith(new HashMap<>(Map.of("question", "q", "documents", docs)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relevant =
                (List<Map<String, Object>>) result.getOutputData().get("relevantDocs");
        assertEquals(2, relevant.size());
        assertEquals(0, result.getOutputData().get("filteredCount"));
    }

    @Test
    void allDocsIrrelevant() {
        List<Map<String, Object>> docs = List.of(
                Map.of("id", "d1", "score", 0.1),
                Map.of("id", "d2", "score", 0.49)
        );

        Task task = taskWith(new HashMap<>(Map.of("question", "q", "documents", docs)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relevant =
                (List<Map<String, Object>>) result.getOutputData().get("relevantDocs");
        assertEquals(0, relevant.size());
        assertEquals(2, result.getOutputData().get("filteredCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
