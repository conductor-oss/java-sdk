package datavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("vd_generate_report", worker.getTaskDefName());
    }

    @Test
    void generatesSummaryForTypicalRun() {
        Task task = taskWith(Map.of(
                "totalRecords", 5,
                "requiredErrors", 1,
                "typeErrors", 0,
                "rangeErrors", 1,
                "validRecords", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Validation complete: 3/5 records valid. Errors: required=1, type=0, range=1",
                result.getOutputData().get("summary"));
        assertEquals("60.0%", result.getOutputData().get("passRate"));
    }

    @Test
    void allRecordsValid() {
        Task task = taskWith(Map.of(
                "totalRecords", 4,
                "requiredErrors", 0,
                "typeErrors", 0,
                "rangeErrors", 0,
                "validRecords", 4));
        TaskResult result = worker.execute(task);

        assertEquals("Validation complete: 4/4 records valid. Errors: required=0, type=0, range=0",
                result.getOutputData().get("summary"));
        assertEquals("100.0%", result.getOutputData().get("passRate"));
    }

    @Test
    void noRecordsValid() {
        Task task = taskWith(Map.of(
                "totalRecords", 3,
                "requiredErrors", 1,
                "typeErrors", 1,
                "rangeErrors", 1,
                "validRecords", 0));
        TaskResult result = worker.execute(task);

        assertEquals("Validation complete: 0/3 records valid. Errors: required=1, type=1, range=1",
                result.getOutputData().get("summary"));
        assertEquals("0.0%", result.getOutputData().get("passRate"));
    }

    @Test
    void handlesZeroTotalRecords() {
        Task task = taskWith(Map.of(
                "totalRecords", 0,
                "requiredErrors", 0,
                "typeErrors", 0,
                "rangeErrors", 0,
                "validRecords", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("0.0%", result.getOutputData().get("passRate"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Validation complete: 0/0 records valid. Errors: required=0, type=0, range=0",
                result.getOutputData().get("summary"));
        assertEquals("0.0%", result.getOutputData().get("passRate"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalRecords", null);
        input.put("requiredErrors", null);
        input.put("typeErrors", null);
        input.put("rangeErrors", null);
        input.put("validRecords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Validation complete: 0/0 records valid. Errors: required=0, type=0, range=0",
                result.getOutputData().get("summary"));
    }

    @Test
    void passRateFormatting() {
        Task task = taskWith(Map.of(
                "totalRecords", 3,
                "requiredErrors", 0,
                "typeErrors", 0,
                "rangeErrors", 1,
                "validRecords", 2));
        TaskResult result = worker.execute(task);

        assertEquals("66.7%", result.getOutputData().get("passRate"));
    }

    @Test
    void singleRecordValid() {
        Task task = taskWith(Map.of(
                "totalRecords", 1,
                "requiredErrors", 0,
                "typeErrors", 0,
                "rangeErrors", 0,
                "validRecords", 1));
        TaskResult result = worker.execute(task);

        assertEquals("Validation complete: 1/1 records valid. Errors: required=0, type=0, range=0",
                result.getOutputData().get("summary"));
        assertEquals("100.0%", result.getOutputData().get("passRate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
