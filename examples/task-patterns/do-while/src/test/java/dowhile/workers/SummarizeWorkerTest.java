package dowhile.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeWorkerTest {

    private final SummarizeWorker worker = new SummarizeWorker();

    @Test
    void taskDefName() {
        assertEquals("dw_summarize", worker.getTaskDefName());
    }

    @Test
    void summarizesFiveIterations() {
        Task task = taskWith(Map.of("totalIterations", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("totalProcessed"));
        assertEquals("Processed 5 items successfully", result.getOutputData().get("summary"));
    }

    @Test
    void summarizesSingleIteration() {
        Task task = taskWith(Map.of("totalIterations", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalProcessed"));
        assertEquals("Processed 1 items successfully", result.getOutputData().get("summary"));
    }

    @Test
    void summarizesZeroIterations() {
        Task task = taskWith(Map.of("totalIterations", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
        assertEquals("Processed 0 items successfully", result.getOutputData().get("summary"));
    }

    @Test
    void defaultsWhenTotalIterationsMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
        assertEquals("Processed 0 items successfully", result.getOutputData().get("summary"));
    }

    @Test
    void defaultsWhenTotalIterationsNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalIterations", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
    }

    @Test
    void handlesLargeBatchSize() {
        Task task = taskWith(Map.of("totalIterations", 1000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1000, result.getOutputData().get("totalProcessed"));
        assertEquals("Processed 1000 items successfully", result.getOutputData().get("summary"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("totalIterations", 3));
        Task task2 = taskWith(Map.of("totalIterations", 3));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("totalProcessed"), result2.getOutputData().get("totalProcessed"));
        assertEquals(result1.getOutputData().get("summary"), result2.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
