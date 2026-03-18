package idempotentprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    private final ProcessWorker worker = new ProcessWorker();

    @Test
    void outputIsDeterministicForSameInput() {
        Task task1 = taskWithInput(Map.of("messageId", "msg-100", "payload", "data"));
        Task task2 = taskWithInput(Map.of("messageId", "msg-100", "payload", "data"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("resultHash"),
                result2.getOutputData().get("resultHash"),
                "Same messageId must produce the same resultHash");
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        Task task1 = taskWithInput(Map.of("messageId", "msg-A", "payload", "data"));
        Task task2 = taskWithInput(Map.of("messageId", "msg-B", "payload", "data"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertNotEquals(result1.getOutputData().get("resultHash"),
                result2.getOutputData().get("resultHash"),
                "Different messageIds must produce different resultHashes");
    }

    @Test
    void requiredOutputFieldsArePresent() {
        Task task = taskWithInput(Map.of("messageId", "msg-200", "payload", "test"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
        assertNotNull(result.getOutputData().get("resultHash"));
        assertTrue(result.getOutputData().get("resultHash").toString().startsWith("sha256:"));
    }

    @Test
    void taskDefNameIsCorrect() {
        assertEquals("idp_process", worker.getTaskDefName());
    }

    private static Task taskWithInput(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setTaskId("test-task-id");
        task.setReferenceTaskName("idp_process_ref");
        task.setWorkflowInstanceId("test-workflow-id");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.getInputData().putAll(input);
        return task;
    }
}
