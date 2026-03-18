package idempotentprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckProcessedWorkerTest {

    private final CheckProcessedWorker worker = new CheckProcessedWorker();

    @BeforeEach
    void setUp() {
        DedupStore.clear();
    }

    @Test
    void newMessageReturnsUnprocessed() {
        Task task = taskWithInput(Map.of("messageId", "msg-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unprocessed", result.getOutputData().get("processingState"));
        assertNull(result.getOutputData().get("previousResult"));
    }

    @Test
    void previouslyRecordedMessageReturnsProcessed() {
        DedupStore.record("msg-002", "sha256:abc123");
        Task task = taskWithInput(Map.of("messageId", "msg-002"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("processingState"));
        assertEquals("sha256:abc123", result.getOutputData().get("previousResult"));
    }

    @Test
    void differentMessageIdsAreIndependent() {
        DedupStore.record("msg-A", "hash-A");
        Task taskA = taskWithInput(Map.of("messageId", "msg-A"));
        Task taskB = taskWithInput(Map.of("messageId", "msg-B"));

        TaskResult resultA = worker.execute(taskA);
        TaskResult resultB = worker.execute(taskB);

        assertEquals("processed", resultA.getOutputData().get("processingState"));
        assertEquals("unprocessed", resultB.getOutputData().get("processingState"));
    }

    @Test
    void taskDefNameIsCorrect() {
        assertEquals("idp_check_processed", worker.getTaskDefName());
    }

    private static Task taskWithInput(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setTaskId("test-task-id");
        task.setReferenceTaskName("idp_check_ref");
        task.setWorkflowInstanceId("test-workflow-id");
        task.getInputData().putAll(input);
        return task;
    }
}
