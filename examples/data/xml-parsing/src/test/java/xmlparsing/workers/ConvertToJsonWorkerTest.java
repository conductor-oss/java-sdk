package xmlparsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConvertToJsonWorkerTest {

    private final ConvertToJsonWorker worker = new ConvertToJsonWorker();

    @Test
    void taskDefName() {
        assertEquals("xp_convert_to_json", worker.getTaskDefName());
    }

    @Test
    void convertsThreeRecords() {
        Task task = taskWith(Map.of("extractedData", sampleRecords()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        assertEquals(3, jsonRecords.size());
    }

    @Test
    void countIsThree() {
        Task task = taskWith(Map.of("extractedData", sampleRecords()));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void addsSourceField() {
        Task task = taskWith(Map.of("extractedData", sampleRecords()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        for (Map<String, Object> record : jsonRecords) {
            assertEquals("xml_feed", record.get("source"));
        }
    }

    @Test
    void addsParsedAtField() {
        Task task = taskWith(Map.of("extractedData", sampleRecords()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        for (Map<String, Object> record : jsonRecords) {
            assertEquals("2026-01-15T10:00:00Z", record.get("parsedAt"));
        }
    }

    @Test
    void preservesOriginalFields() {
        Task task = taskWith(Map.of("extractedData", sampleRecords()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        Map<String, Object> first = jsonRecords.get(0);
        assertEquals("P-101", first.get("id"));
        assertEquals("Laptop", first.get("name"));
        assertEquals(999.99, first.get("price"));
        assertEquals("electronics", first.get("category"));
    }

    @Test
    void handlesNullExtractedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("extractedData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        assertEquals(0, jsonRecords.size());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonRecords = (List<Map<String, Object>>) result.getOutputData().get("jsonRecords");
        assertEquals(0, jsonRecords.size());
    }

    @Test
    void handlesEmptyExtractedData() {
        Task task = taskWith(Map.of("extractedData", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    private List<Map<String, Object>> sampleRecords() {
        return List.of(
                Map.of("id", "P-101", "name", "Laptop", "price", 999.99, "category", "electronics"),
                Map.of("id", "P-102", "name", "Headphones", "price", 149.99, "category", "audio"),
                Map.of("id", "P-103", "name", "Keyboard", "price", 79.99, "category", "peripherals")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
