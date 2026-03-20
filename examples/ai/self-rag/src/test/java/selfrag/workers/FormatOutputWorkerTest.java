package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatOutputWorkerTest {

    private final FormatOutputWorker worker = new FormatOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_format_output", worker.getTaskDefName());
    }

    @Test
    void returnsAnswerAndSourceCount() {
        List<Map<String, Object>> sources = List.of(
                Map.of("id", "d1", "text", "doc1", "score", 0.91),
                Map.of("id", "d2", "text", "doc2", "score", 0.85),
                Map.of("id", "d3", "text", "doc3", "score", 0.78)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "answer", "Test answer",
                "halScore", 0.92,
                "useScore", 0.88,
                "sources", sources)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Test answer", result.getOutputData().get("answer"));
        assertEquals(3, result.getOutputData().get("sourceCount"));
    }

    @Test
    void sourceCountMatchesInputSize() {
        List<Map<String, Object>> sources = List.of(
                Map.of("id", "d1", "text", "doc1", "score", 0.9)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "answer", "answer",
                "halScore", 0.92,
                "useScore", 0.88,
                "sources", sources)));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("sourceCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
