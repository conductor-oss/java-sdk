package idempotentprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordWorkerTest {

    private final RecordWorker recordWorker = new RecordWorker();
    private final CheckProcessedWorker checkWorker = new CheckProcessedWorker();

    @BeforeEach
    void setUp() {
        DedupStore.clear();
    }

    @Test
    void recordingMarksMessageAsProcessed() {
        String messageId = "msg-rec-001";
        String resultHash = "sha256:abc";

        assertFalse(DedupStore.isProcessed(messageId));

        Task task = taskWithInput(Map.of("messageId", messageId, "resultHash", resultHash));
        TaskResult result = recordWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
        assertTrue(DedupStore.isProcessed(messageId));
        assertEquals(resultHash, DedupStore.getResultHash(messageId));
    }

    @Test
    void canBeVerifiedViaCheckProcessedWorker() {
        String messageId = "msg-rec-002";

        Task checkTask1 = checkTask(messageId);
        TaskResult checkResult1 = checkWorker.execute(checkTask1);
        assertEquals("unprocessed", checkResult1.getOutputData().get("processingState"));

        Task recordTask = taskWithInput(Map.of("messageId", messageId, "resultHash", "sha256:xyz"));
        recordWorker.execute(recordTask);

        Task checkTask2 = checkTask(messageId);
        TaskResult checkResult2 = checkWorker.execute(checkTask2);
        assertEquals("processed", checkResult2.getOutputData().get("processingState"));
        assertEquals("sha256:xyz", checkResult2.getOutputData().get("previousResult"));
    }

    @Test
    void taskDefNameIsCorrect() {
        assertEquals("idp_record", recordWorker.getTaskDefName());
    }

    private static Task taskWithInput(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setTaskId("test-task-id");
        task.setReferenceTaskName("idp_record_ref");
        task.setWorkflowInstanceId("test-workflow-id");
        task.getInputData().putAll(input);
        return task;
    }

    private static Task checkTask(String messageId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setTaskId("test-task-id");
        task.setReferenceTaskName("idp_check_ref");
        task.setWorkflowInstanceId("test-workflow-id");
        task.getInputData().put("messageId", messageId);
        return task;
    }
}
