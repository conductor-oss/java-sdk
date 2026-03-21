package dataclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ApplyLabelsWorkerTest {

    private final ApplyLabelsWorker worker = new ApplyLabelsWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_apply_labels", worker.getTaskDefName());
    }

    @Test
    void appliesLabelsSuccessfully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("apply_labels"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void completedAtIsTimestamp() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        String completedAt = (String) result.getOutputData().get("completedAt");
        assertNotNull(completedAt);
        assertTrue(completedAt.contains("T"), "completedAt should be ISO timestamp");
    }

    @Test
    void outputContainsBothExpectedKeys() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("apply_labels"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }
}
