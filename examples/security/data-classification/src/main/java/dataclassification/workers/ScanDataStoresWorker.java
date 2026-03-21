package dataclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Scans data stores for schema and column information.
 * Input: dataStore, scanType
 * Output: scan_data_storesId, success
 */
public class ScanDataStoresWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_scan_data_stores";
    }

    @Override
    public TaskResult execute(Task task) {
        String dataStore = (String) task.getInputData().get("dataStore");
        if (dataStore == null) {
            dataStore = "unknown";
        }

        System.out.println("  [scan] Scanned " + dataStore + ": 42 tables, 318 columns");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scan_data_storesId", "SCAN_DATA_STORES-1384");
        result.getOutputData().put("success", true);
        return result;
    }
}
