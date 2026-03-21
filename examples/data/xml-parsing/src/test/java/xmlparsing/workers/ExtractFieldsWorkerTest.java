package xmlparsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractFieldsWorkerTest {

    private final ExtractFieldsWorker worker = new ExtractFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("xp_extract_fields", worker.getTaskDefName());
    }

    @Test
    void extractsThreeRecords() {
        Task task = taskWith(Map.of("elements", sampleElements()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(3, records.size());
    }

    @Test
    void fieldCountIsTwelve() {
        Task task = taskWith(Map.of("elements", sampleElements()));
        TaskResult result = worker.execute(task);

        assertEquals(12, result.getOutputData().get("fieldCount"));
    }

    @Test
    void firstRecordHasCorrectFields() {
        Task task = taskWith(Map.of("elements", sampleElements()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        Map<String, Object> first = records.get(0);
        assertEquals("P-101", first.get("id"));
        assertEquals("Laptop", first.get("name"));
        assertEquals(999.99, first.get("price"));
        assertEquals("electronics", first.get("category"));
    }

    @Test
    void secondRecordHasCorrectFields() {
        Task task = taskWith(Map.of("elements", sampleElements()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        Map<String, Object> second = records.get(1);
        assertEquals("P-102", second.get("id"));
        assertEquals("Headphones", second.get("name"));
        assertEquals(149.99, second.get("price"));
        assertEquals("audio", second.get("category"));
    }

    @Test
    void priceIsDouble() {
        Task task = taskWith(Map.of("elements", sampleElements()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        for (Map<String, Object> record : records) {
            assertInstanceOf(Double.class, record.get("price"));
        }
    }

    @Test
    void handlesNullElements() {
        Map<String, Object> input = new HashMap<>();
        input.put("elements", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(0, records.size());
        assertEquals(0, result.getOutputData().get("fieldCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(0, records.size());
    }

    @Test
    void handlesEmptyElementsList() {
        Task task = taskWith(Map.of("elements", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(0, records.size());
        assertEquals(0, result.getOutputData().get("fieldCount"));
    }

    private List<Map<String, Object>> sampleElements() {
        return List.of(
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-101"),
                        "children", Map.of("name", "Laptop", "price", "999.99", "category", "electronics")
                ),
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-102"),
                        "children", Map.of("name", "Headphones", "price", "149.99", "category", "audio")
                ),
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-103"),
                        "children", Map.of("name", "Keyboard", "price", "79.99", "category", "peripherals")
                )
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
