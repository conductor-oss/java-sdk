package setvariable.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessItemsWorkerTest {

    private final ProcessItemsWorker worker = new ProcessItemsWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_process_items", worker.getTaskDefName());
    }

    @Test
    void computesTotalAndCountAndCategory() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Server License", "amount", 2500),
                Map.of("name", "Support Plan", "amount", 800),
                Map.of("name", "Training", "amount", 1200)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4500.0, result.getOutputData().get("totalAmount"));
        assertEquals(3, result.getOutputData().get("itemCount"));
        assertEquals("high-value", result.getOutputData().get("category"));
    }

    @Test
    void categorizesMicroForSmallAmounts() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Sticker", "amount", 5),
                Map.of("name", "Pen", "amount", 3)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(8.0, result.getOutputData().get("totalAmount"));
        assertEquals(2, result.getOutputData().get("itemCount"));
        assertEquals("micro", result.getOutputData().get("category"));
    }

    @Test
    void categorizesStandardForMediumAmounts() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Book", "amount", 150),
                Map.of("name", "Course", "amount", 300)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(450.0, result.getOutputData().get("totalAmount"));
        assertEquals("standard", result.getOutputData().get("category"));
    }

    @Test
    void failsWhenNoItemsProvided() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("No items provided", result.getOutputData().get("error"));
    }

    @Test
    void failsWhenItemsListIsEmpty() {
        Task task = taskWith(Map.of("items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
