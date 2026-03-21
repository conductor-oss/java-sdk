package supplychainmgmt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanWorkerTest {

    private final PlanWorker worker = new PlanWorker();

    @Test
    void taskDefName() {
        assertEquals("scm_plan", worker.getTaskDefName());
    }

    @Test
    void createsPlanWithMaterials() {
        Task task = taskWith(Map.of("product", "Sensor", "quantity", 100));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PLN-651", result.getOutputData().get("planId"));
        assertNotNull(result.getOutputData().get("materials"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void materialsQuantityScalesWithInput() {
        Task task = taskWith(Map.of("product", "Widget", "quantity", 50));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> materials =
                (List<Map<String, Object>>) result.getOutputData().get("materials");
        assertEquals(3, materials.size());
        assertEquals(100, materials.get(0).get("qty")); // steel = qty * 2
        assertEquals(50, materials.get(1).get("qty"));  // electronics = qty
        assertEquals(50, materials.get(2).get("qty"));  // packaging = qty
    }

    @Test
    void handlesZeroQuantity() {
        Task task = taskWith(Map.of("product", "Test", "quantity", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
