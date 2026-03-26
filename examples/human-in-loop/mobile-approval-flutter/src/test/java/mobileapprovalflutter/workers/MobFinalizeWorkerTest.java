package mobileapprovalflutter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobFinalizeWorkerTest {

    @Test
    void taskDefName() {
        MobFinalizeWorker worker = new MobFinalizeWorker();
        assertEquals("mob_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsDoneTrue() {
        MobFinalizeWorker worker = new MobFinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("requestId", "REQ-001", "approved", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void returnsDoneTrueWithEmptyInput() {
        MobFinalizeWorker worker = new MobFinalizeWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void outputContainsDoneKey() {
        MobFinalizeWorker worker = new MobFinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("approved", false)));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("done"));
    }

    @Test
    void alwaysCompletes() {
        MobFinalizeWorker worker = new MobFinalizeWorker();
        Task task1 = taskWith(new HashMap<>());
        Task task2 = taskWith(new HashMap<>(Map.of("requestId", "REQ-C", "approved", true)));

        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task1).getStatus());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task2).getStatus());
    }

    @Test
    void completesRegardlessOfApprovalDecision() {
        MobFinalizeWorker worker = new MobFinalizeWorker();

        Task approvedTask = taskWith(new HashMap<>(Map.of("approved", true)));
        TaskResult approvedResult = worker.execute(approvedTask);
        assertEquals(TaskResult.Status.COMPLETED, approvedResult.getStatus());
        assertEquals(true, approvedResult.getOutputData().get("done"));

        Task rejectedTask = taskWith(new HashMap<>(Map.of("approved", false)));
        TaskResult rejectedResult = worker.execute(rejectedTask);
        assertEquals(TaskResult.Status.COMPLETED, rejectedResult.getStatus());
        assertEquals(true, rejectedResult.getOutputData().get("done"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
