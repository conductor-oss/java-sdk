package dataclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {

    private final ClassifyWorker worker = new ClassifyWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_classify", worker.getTaskDefName());
    }

    @Test
    void classifiesSuccessfully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("classify"));
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

        assertTrue(result.getOutputData().containsKey("classify"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }
}
