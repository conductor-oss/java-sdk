package xmlparsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitRecordsWorkerTest {

    private final EmitRecordsWorker worker = new EmitRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("xp_emit_records", worker.getTaskDefName());
    }

    @Test
    void statusIsXmlToJsonComplete() {
        Task task = taskWith(Map.of(
                "jsonRecords", List.of(Map.of("id", "P-101")),
                "recordCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("XML_TO_JSON_COMPLETE", result.getOutputData().get("status"));
    }

    @Test
    void passesRecordCountThrough() {
        Task task = taskWith(Map.of(
                "jsonRecords", List.of(),
                "recordCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesZeroRecordCount() {
        Task task = taskWith(Map.of(
                "jsonRecords", List.of(),
                "recordCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("XML_TO_JSON_COMPLETE", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesLargeRecordCount() {
        Task task = taskWith(Map.of(
                "jsonRecords", List.of(),
                "recordCount", 1000));
        TaskResult result = worker.execute(task);

        assertEquals(1000, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesNullRecordCount() {
        Map<String, Object> input = new HashMap<>();
        input.put("jsonRecords", List.of());
        input.put("recordCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("XML_TO_JSON_COMPLETE", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of(
                "jsonRecords", List.of(Map.of("id", "P-101"), Map.of("id", "P-102")),
                "recordCount", 2));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("status"));
        assertNotNull(result.getOutputData().get("recordCount"));
        assertEquals(2, result.getOutputData().size());
    }

    @Test
    void statusIsAlwaysConstant() {
        Task task1 = taskWith(Map.of("jsonRecords", List.of(), "recordCount", 1));
        Task task2 = taskWith(Map.of("jsonRecords", List.of(), "recordCount", 5));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("status"), result2.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
