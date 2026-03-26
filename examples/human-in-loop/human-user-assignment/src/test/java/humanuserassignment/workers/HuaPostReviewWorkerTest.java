package humanuserassignment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HuaPostReviewWorkerTest {

    @Test
    void taskDefName() {
        HuaPostReviewWorker worker = new HuaPostReviewWorker();
        assertEquals("hua_post_review", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        HuaPostReviewWorker worker = new HuaPostReviewWorker();
        Task task = taskWith(Map.of("reviewResult", "approved", "documentId", "DOC-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsFinalizedTrue() {
        HuaPostReviewWorker worker = new HuaPostReviewWorker();
        Task task = taskWith(Map.of("reviewResult", "approved", "documentId", "DOC-002"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void worksWithEmptyInput() {
        HuaPostReviewWorker worker = new HuaPostReviewWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void deterministicOutput() {
        HuaPostReviewWorker worker = new HuaPostReviewWorker();
        Task task1 = taskWith(Map.of("reviewResult", "rejected"));
        Task task2 = taskWith(Map.of("reviewResult", "rejected"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getStatus(), result2.getStatus());
        assertEquals(result1.getOutputData().get("finalized"), result2.getOutputData().get("finalized"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
