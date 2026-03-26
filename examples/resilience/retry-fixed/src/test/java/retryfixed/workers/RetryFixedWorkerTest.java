package retryfixed.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryFixedWorkerTest {

    @Test
    void taskDefName() {
        RetryFixedWorker worker = new RetryFixedWorker();
        assertEquals("retry_fixed_task", worker.getTaskDefName());
    }

    @Test
    void succeedsImmediatelyWhenFailCountIsZero() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of("failCount", 0));
        task.setWorkflowInstanceId("wf-zero");
        TaskResult result = freshWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void succeedsWhenNoFailCountProvided() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of());
        task.setWorkflowInstanceId("wf-no-input");
        TaskResult result = freshWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void failsOnFirstAttemptWhenFailCountIsOne() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of("failCount", 1));
        task.setWorkflowInstanceId("wf-fail-once");
        TaskResult result = freshWorker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void succeedsOnSecondAttemptAfterOneFailure() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task1 = taskWith(Map.of("failCount", 1));
        task1.setWorkflowInstanceId("wf-recover");
        freshWorker.execute(task1);

        Task task2 = taskWith(Map.of("failCount", 1));
        task2.setWorkflowInstanceId("wf-recover");
        TaskResult result = freshWorker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("attempts"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsAttemptsOnSuccess() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of("failCount", 0));
        task.setWorkflowInstanceId("wf-output-check");
        TaskResult result = freshWorker.execute(task);

        assertTrue(result.getOutputData().containsKey("attempts"));
        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void outputContainsErrorOnFailure() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of("failCount", 3));
        task.setWorkflowInstanceId("wf-error-check");
        TaskResult result = freshWorker.execute(task);

        assertTrue(result.getOutputData().containsKey("error"));
        assertTrue(result.getOutputData().containsKey("attempts"));
    }

    @Test
    void handlesNonNumericFailCountGracefully() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();
        Task task = taskWith(Map.of("failCount", "not-a-number"));
        task.setWorkflowInstanceId("wf-non-numeric");
        TaskResult result = freshWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void isolatesAttemptCountsBetweenWorkflows() {
        RetryFixedWorker freshWorker = new RetryFixedWorker();

        Task taskA = taskWith(Map.of("failCount", 2));
        taskA.setWorkflowInstanceId("wf-a");
        freshWorker.execute(taskA);

        Task taskB = taskWith(Map.of("failCount", 0));
        taskB.setWorkflowInstanceId("wf-b");
        TaskResult resultB = freshWorker.execute(taskB);

        assertEquals(TaskResult.Status.COMPLETED, resultB.getStatus());
        assertEquals(1, resultB.getOutputData().get("attempts"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
