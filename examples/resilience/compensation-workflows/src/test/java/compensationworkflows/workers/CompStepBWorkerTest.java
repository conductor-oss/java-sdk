package compensationworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompStepBWorkerTest {

    @BeforeEach
    void setUp() {
        CompStepBWorker.clearRecords();
    }

    @Test
    void taskDefName() {
        CompStepBWorker worker = new CompStepBWorker();
        assertEquals("comp_step_b", worker.getTaskDefName());
    }

    @Test
    void insertsRecordIntoStore() {
        CompStepBWorker worker = new CompStepBWorker();
        Task task = taskWith(Map.of("step", "B"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("record-B-inserted", result.getOutputData().get("result"));

        String recordKey = (String) result.getOutputData().get("recordKey");
        assertNotNull(recordKey);
        assertTrue(CompStepBWorker.RECORDS.containsKey(recordKey),
                "Record should exist in store");
    }

    @Test
    void outputContainsResultKey() {
        CompStepBWorker worker = new CompStepBWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void worksWithEmptyInput() {
        CompStepBWorker worker = new CompStepBWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("record-B-inserted", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
