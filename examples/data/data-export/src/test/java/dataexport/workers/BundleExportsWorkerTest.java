package dataexport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BundleExportsWorkerTest {

    private final BundleExportsWorker worker = new BundleExportsWorker();

    @Test
    void taskDefName() {
        assertEquals("dx_bundle_exports", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of(
                "csvFile", "export/data.csv",
                "jsonFile", "export/data.json",
                "excelFile", "export/data.xlsx",
                "destination", "s3://exports"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsBundleUrl() {
        Task task = taskWith(Map.of(
                "csvFile", "data.csv", "jsonFile", "data.json",
                "excelFile", "data.xlsx", "destination", "s3://my-bucket"));
        TaskResult result = worker.execute(task);
        String bundleUrl = (String) result.getOutputData().get("bundleUrl");
        assertTrue(bundleUrl.startsWith("s3://my-bucket/"));
    }

    @Test
    void returnsExportCount() {
        Task task = taskWith(Map.of(
                "csvFile", "a.csv", "jsonFile", "a.json",
                "excelFile", "a.xlsx", "destination", "s3://exports"));
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("exportCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsFilesList() {
        Task task = taskWith(Map.of(
                "csvFile", "data.csv", "jsonFile", "data.json",
                "excelFile", "data.xlsx", "destination", "s3://exports"));
        TaskResult result = worker.execute(task);
        List<String> files = (List<String>) result.getOutputData().get("files");
        assertEquals(3, files.size());
    }

    @Test
    void usesDefaultDestination() {
        Task task = taskWith(Map.of("csvFile", "a", "jsonFile", "b", "excelFile", "c"));
        TaskResult result = worker.execute(task);
        String bundleUrl = (String) result.getOutputData().get("bundleUrl");
        assertTrue(bundleUrl.startsWith("s3://exports/"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
