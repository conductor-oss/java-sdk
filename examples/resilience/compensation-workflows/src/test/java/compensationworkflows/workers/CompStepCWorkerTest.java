package compensationworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompStepCWorkerTest {

    @Test
    void taskDefName() {
        CompStepCWorker worker = new CompStepCWorker();
        assertEquals("comp_step_c", worker.getTaskDefName());
    }

    @Test
    void succeedsWhenFailAtStepIsNotC() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("step", "C", "failAtStep", "none", "prevResult", "record-B-inserted"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification-C-sent", result.getOutputData().get("result"));
    }

    @Test
    void failsWhenFailAtStepIsC() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("step", "C", "failAtStep", "C", "prevResult", "record-B-inserted"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getReasonForIncompletion());
        assertEquals("External service unavailable", result.getReasonForIncompletion());
    }

    @Test
    void succeedsWithEmptyFailAtStep() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("step", "C"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification-C-sent", result.getOutputData().get("result"));
    }

    @Test
    void succeedsWhenFailAtStepIsA() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("failAtStep", "A"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification-C-sent", result.getOutputData().get("result"));
    }

    @Test
    void succeedsWhenFailAtStepIsB() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("failAtStep", "B"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification-C-sent", result.getOutputData().get("result"));
    }

    @Test
    void failedResultHasNoOutputResult() {
        CompStepCWorker worker = new CompStepCWorker();
        Task task = taskWith(Map.of("failAtStep", "C"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertFalse(result.getOutputData().containsKey("result"));
    }

    @Test
    void succeedsWithNullFailAtStep() {
        CompStepCWorker worker = new CompStepCWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("failAtStep", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notification-C-sent", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
