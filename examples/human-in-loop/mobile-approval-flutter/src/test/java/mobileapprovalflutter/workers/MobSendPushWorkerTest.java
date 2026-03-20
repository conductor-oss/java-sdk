package mobileapprovalflutter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobSendPushWorkerTest {

    @Test
    void taskDefName() {
        MobSendPushWorker worker = new MobSendPushWorker();
        assertEquals("mob_send_push", worker.getTaskDefName());
    }

    @Test
    void returnsPushSentTrue() {
        MobSendPushWorker worker = new MobSendPushWorker();
        Task task = taskWith(new HashMap<>(Map.of("requestId", "REQ-001", "userId", "user-42")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("pushSent"));
    }

    @Test
    void returnsPushSentTrueWithEmptyInput() {
        MobSendPushWorker worker = new MobSendPushWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("pushSent"));
    }

    @Test
    void outputContainsPushSentKey() {
        MobSendPushWorker worker = new MobSendPushWorker();
        Task task = taskWith(new HashMap<>(Map.of("userId", "user-99")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("pushSent"));
    }

    @Test
    void alwaysCompletes() {
        MobSendPushWorker worker = new MobSendPushWorker();
        Task task1 = taskWith(new HashMap<>());
        Task task2 = taskWith(new HashMap<>(Map.of("requestId", "REQ-B")));

        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task1).getStatus());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task2).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
