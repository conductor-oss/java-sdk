package dataclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScanDataStoresWorkerTest {

    private final ScanDataStoresWorker worker = new ScanDataStoresWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_scan_data_stores", worker.getTaskDefName());
    }

    @Test
    void scansValidDataStore() {
        Task task = taskWith("production-db");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
        assertNotNull(result.getOutputData().get("scan_data_storesId"));
    }

    @Test
    void defaultsDataStoreToUnknown() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void scanIdHasExpectedPrefix() {
        Task task = taskWith("analytics-warehouse");

        TaskResult result = worker.execute(task);

        String scanId = (String) result.getOutputData().get("scan_data_storesId");
        assertNotNull(scanId);
        assertTrue(scanId.startsWith("SCAN_DATA_STORES-"));
    }

    private Task taskWith(String dataStore) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("dataStore", dataStore);
        task.setInputData(input);
        return task;
    }
}
