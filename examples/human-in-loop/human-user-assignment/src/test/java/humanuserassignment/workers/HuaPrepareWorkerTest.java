package humanuserassignment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HuaPrepareWorkerTest {

    @Test
    void taskDefName() {
        HuaPrepareWorker worker = new HuaPrepareWorker();
        assertEquals("hua_prepare", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        HuaPrepareWorker worker = new HuaPrepareWorker();
        Task task = taskWith(Map.of("documentId", "DOC-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsPreparedTrue() {
        HuaPrepareWorker worker = new HuaPrepareWorker();
        Task task = taskWith(Map.of("documentId", "DOC-002"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void worksWithEmptyInput() {
        HuaPrepareWorker worker = new HuaPrepareWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void deterministicOutput() {
        HuaPrepareWorker worker = new HuaPrepareWorker();
        Task task1 = taskWith(Map.of("documentId", "DOC-X"));
        Task task2 = taskWith(Map.of("documentId", "DOC-X"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getStatus(), result2.getStatus());
        assertEquals(result1.getOutputData().get("prepared"), result2.getOutputData().get("prepared"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
