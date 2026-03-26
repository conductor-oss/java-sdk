package dataclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DetectPiiWorkerTest {

    private final DetectPiiWorker worker = new DetectPiiWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_detect_pii", worker.getTaskDefName());
    }

    @Test
    void detectsPii() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("detect_pii"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void alwaysMarkedAsProcessed() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsBothExpectedKeys() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("detect_pii"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }
}
