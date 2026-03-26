package datawarehouseload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StageDataWorkerTest {

    private final StageDataWorker worker = new StageDataWorker();

    @Test
    void taskDefName() {
        assertEquals("wh_stage_data", worker.getTaskDefName());
    }

    @Test
    void stagesMultipleRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)),
                "targetTable", "fact_revenue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("stagedCount"));
        String stagingTable = (String) result.getOutputData().get("stagingTable");
        assertTrue(stagingTable.startsWith("stg_fact_revenue_"));
    }

    @Test
    void stagesSingleRecord() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "targetTable", "dim_customer"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("stagedCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(),
                "targetTable", "fact_orders"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("stagedCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("targetTable", "fact_data");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("stagedCount"));
    }

    @Test
    void stagingTableContainsTargetName() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "targetTable", "my_table"));
        TaskResult result = worker.execute(task);

        String stagingTable = (String) result.getOutputData().get("stagingTable");
        assertTrue(stagingTable.contains("my_table"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
