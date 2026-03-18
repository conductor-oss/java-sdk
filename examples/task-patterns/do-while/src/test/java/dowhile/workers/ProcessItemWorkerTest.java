package dowhile.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessItemWorkerTest {

    private final ProcessItemWorker worker = new ProcessItemWorker();

    @Test
    void taskDefName() {
        assertEquals("dw_process_item", worker.getTaskDefName());
    }

    @Test
    void processesItemAtIterationZero() {
        Task task = taskWith(Map.of("iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("itemProcessed"));
        assertEquals(1, result.getOutputData().get("iteration"));
        assertEquals("Item-1 processed", result.getOutputData().get("result"));
    }

    @Test
    void processesItemAtIterationThree() {
        Task task = taskWith(Map.of("iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("itemProcessed"));
        assertEquals(4, result.getOutputData().get("iteration"));
        assertEquals("Item-4 processed", result.getOutputData().get("result"));
    }

    @Test
    void normalIterationWithBatchOfFive() {
        // Run 5 iterations of the loop
        int iteration = 0;
        for (int i = 0; i < 5; i++) {
            Task task = taskWith(Map.of("iteration", iteration));
            TaskResult result = worker.execute(task);

            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
            assertEquals(true, result.getOutputData().get("itemProcessed"));
            iteration = (int) result.getOutputData().get("iteration");
        }
        assertEquals(5, iteration);
    }

    @Test
    void singleIteration() {
        Task task = taskWith(Map.of("iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
        assertEquals("Item-1 processed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsIterationWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
        assertEquals("Item-1 processed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsIterationWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("iteration", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesIterationAsString() {
        // In case the iteration comes in as a non-Number type, defaults to 0
        Task task = taskWith(Map.of("iteration", "not-a-number"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of("iteration", 2));
        Task task2 = taskWith(Map.of("iteration", 2));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("iteration"), result2.getOutputData().get("iteration"));
        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
        assertEquals(result1.getOutputData().get("itemProcessed"), result2.getOutputData().get("itemProcessed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
