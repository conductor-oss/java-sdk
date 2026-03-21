package datawarehouseload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpsertTargetWorkerTest {

    private final UpsertTargetWorker worker = new UpsertTargetWorker();

    @Test
    void taskDefName() {
        assertEquals("wh_upsert_target", worker.getTaskDefName());
    }

    @Test
    void upsertsRecords() {
        Task task = taskWith(Map.of("recordCount", 10, "targetTable", "fact_revenue", "stagingTable", "stg_test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("upsertedCount"));
    }

    @Test
    void insertedAndUpdatedSumToTotal() {
        Task task = taskWith(Map.of("recordCount", 10, "targetTable", "fact_revenue"));
        TaskResult result = worker.execute(task);

        int inserted = (int) result.getOutputData().get("inserted");
        int updated = (int) result.getOutputData().get("updated");
        assertEquals(10, inserted + updated);
    }

    @Test
    void sixtyPercentInserted() {
        Task task = taskWith(Map.of("recordCount", 10, "targetTable", "fact_test"));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("inserted"));
        assertEquals(4, result.getOutputData().get("updated"));
    }

    @Test
    void handlesZeroRecords() {
        Task task = taskWith(Map.of("recordCount", 0, "targetTable", "fact_empty"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("upsertedCount"));
    }

    @Test
    void outputContainsTargetTable() {
        Task task = taskWith(Map.of("recordCount", 5, "targetTable", "dim_customer"));
        TaskResult result = worker.execute(task);

        assertEquals("dim_customer", result.getOutputData().get("targetTable"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
