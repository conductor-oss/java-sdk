package kycaml.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScreenWatchlistsWorkerTest {

    private final ScreenWatchlistsWorker worker = new ScreenWatchlistsWorker();

    @Test
    void taskDefName() {
        assertEquals("kyc_screen_watchlists", worker.getTaskDefName());
    }

    @Test
    void screensCustomerSuccessfully() {
        Task task = taskWith(Map.of("name", "John Anderson", "nationality", "US", "customerId", "CUST-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("hits"));
        assertEquals("clear", result.getOutputData().get("clearanceStatus"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void checksMultipleLists() {
        Task task = taskWith(Map.of("name", "Jane Doe", "nationality", "US", "customerId", "CUST-2"));
        TaskResult result = worker.execute(task);

        List<String> lists = (List<String>) result.getOutputData().get("listsChecked");
        assertNotNull(lists);
        assertTrue(lists.size() >= 4);
        assertTrue(lists.contains("OFAC_SDN"));
    }

    @Test
    void handlesNullName() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", null);
        input.put("nationality", "US");
        Task task = taskWith(input);
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
