package exclusivejoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectBestWorkerTest {

    private final SelectBestWorker worker = new SelectBestWorker();

    @Test
    void taskDefName() {
        assertEquals("ej_select_best", worker.getTaskDefName());
    }

    @Test
    void selectsLowestPriceVendor() {
        Map<String, Object> vendorA = vendorData("A", 59.99, 250);
        Map<String, Object> vendorB = vendorData("B", 42.50, 450);
        Map<String, Object> vendorC = vendorData("C", 55.00, 150);

        Task task = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> bestVendor = (Map<String, Object>) result.getOutputData().get("bestVendor");
        assertNotNull(bestVendor);
        assertEquals("B", bestVendor.get("winner"));
        assertEquals(42.50, bestVendor.get("price"));
        assertEquals(450, bestVendor.get("responseTime"));
    }

    @Test
    void selectsFastestWhenPricesTied() {
        Map<String, Object> vendorA = vendorData("A", 50.00, 300);
        Map<String, Object> vendorB = vendorData("B", 50.00, 100);
        Map<String, Object> vendorC = vendorData("C", 50.00, 200);

        Task task = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestVendor = (Map<String, Object>) result.getOutputData().get("bestVendor");
        assertEquals("B", bestVendor.get("winner"));
        assertEquals(100, bestVendor.get("responseTime"));
    }

    @Test
    void selectsVendorAWhenCheapest() {
        Map<String, Object> vendorA = vendorData("A", 10.00, 500);
        Map<String, Object> vendorB = vendorData("B", 20.00, 100);
        Map<String, Object> vendorC = vendorData("C", 30.00, 200);

        Task task = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestVendor = (Map<String, Object>) result.getOutputData().get("bestVendor");
        assertEquals("A", bestVendor.get("winner"));
        assertEquals(10.00, bestVendor.get("price"));
    }

    @Test
    void selectsVendorCWhenCheapest() {
        Map<String, Object> vendorA = vendorData("A", 99.99, 100);
        Map<String, Object> vendorB = vendorData("B", 88.88, 200);
        Map<String, Object> vendorC = vendorData("C", 11.11, 300);

        Task task = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestVendor = (Map<String, Object>) result.getOutputData().get("bestVendor");
        assertEquals("C", bestVendor.get("winner"));
        assertEquals(11.11, bestVendor.get("price"));
    }

    @Test
    void outputContainsExpectedFields() {
        Map<String, Object> vendorA = vendorData("A", 59.99, 250);
        Map<String, Object> vendorB = vendorData("B", 42.50, 450);
        Map<String, Object> vendorC = vendorData("C", 55.00, 150);

        Task task = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestVendor = (Map<String, Object>) result.getOutputData().get("bestVendor");
        assertEquals(3, bestVendor.size());
        assertTrue(bestVendor.containsKey("winner"));
        assertTrue(bestVendor.containsKey("price"));
        assertTrue(bestVendor.containsKey("responseTime"));
    }

    @Test
    void returnsDeterministicResults() {
        Map<String, Object> vendorA = vendorData("A", 59.99, 250);
        Map<String, Object> vendorB = vendorData("B", 42.50, 450);
        Map<String, Object> vendorC = vendorData("C", 55.00, 150);

        Task task1 = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        Task task2 = taskWith(Map.of("vendorA", vendorA, "vendorB", vendorB, "vendorC", vendorC));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("bestVendor"),
                result2.getOutputData().get("bestVendor"));
    }

    private Map<String, Object> vendorData(String vendor, double price, int responseTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vendor", vendor);
        data.put("price", price);
        data.put("responseTime", responseTime);
        data.put("query", "wireless-keyboard");
        return data;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
