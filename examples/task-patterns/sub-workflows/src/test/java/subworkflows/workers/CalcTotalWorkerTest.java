package subworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalcTotalWorkerTest {

    private final CalcTotalWorker worker = new CalcTotalWorker();

    @Test
    void taskDefName() {
        assertEquals("sub_calc_total", worker.getTaskDefName());
    }

    @Test
    void calculatesCorrectTotal() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Widget", "price", 10.0, "qty", 2),
                Map.of("name", "Gadget", "price", 25.0, "qty", 1),
                Map.of("name", "Doohickey", "price", 5.0, "qty", 4)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // 10*2 + 25*1 + 5*4 = 20 + 25 + 20 = 65
        assertEquals(65.0, result.getOutputData().get("total"));
        assertEquals(3, result.getOutputData().get("itemCount"));
    }

    @Test
    void singleItem() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Single", "price", 42.5, "qty", 3)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(127.5, result.getOutputData().get("total"));
        assertEquals(1, result.getOutputData().get("itemCount"));
    }

    @Test
    void emptyItemsList() {
        Task task = taskWith(Map.of("items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("total"));
        assertEquals(0, result.getOutputData().get("itemCount"));
    }

    @Test
    void nullItemsList() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("total"));
        assertEquals(0, result.getOutputData().get("itemCount"));
    }

    @Test
    void handlesIntegerPrices() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "IntItem", "price", 10, "qty", 5)
        );
        Task task = taskWith(Map.of("items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(50.0, result.getOutputData().get("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
