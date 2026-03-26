package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RefineRetryWorkerTest {

    private final RefineRetryWorker worker = new RefineRetryWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_refine_retry", worker.getTaskDefName());
    }

    @Test
    void returnsRefinedQuery() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What task types does Conductor support?",
                "halScore", 0.3,
                "useScore", 0.4)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("What task types does Conductor support? (refined)",
                result.getOutputData().get("refinedQuery"));
    }

    @Test
    void refinedQueryAppendsToOriginal() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "my question",
                "halScore", 0.1,
                "useScore", 0.2)));
        TaskResult result = worker.execute(task);

        String refined = (String) result.getOutputData().get("refinedQuery");
        assertTrue(refined.startsWith("my question"));
        assertTrue(refined.endsWith("(refined)"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
