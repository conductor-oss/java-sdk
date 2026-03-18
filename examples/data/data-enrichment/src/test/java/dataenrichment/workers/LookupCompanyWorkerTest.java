package dataenrichment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupCompanyWorkerTest {

    private final LookupCompanyWorker worker = new LookupCompanyWorker();

    @Test
    void taskDefName() {
        assertEquals("dr_lookup_company", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesAcmeEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "alice@acme.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> company = (Map<String, Object>) enriched.get(0).get("company");
        assertEquals("Acme Corp", company.get("company"));
        assertEquals("Technology", company.get("industry"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesGlobexEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 2, "email", "bob@globex.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> company = (Map<String, Object>) enriched.get(0).get("company");
        assertEquals("Globex Inc", company.get("company"));
        assertEquals("Finance", company.get("industry"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesInitechEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 3, "email", "charlie@initech.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> company = (Map<String, Object>) enriched.get(0).get("company");
        assertEquals("Initech", company.get("company"));
        assertEquals("Consulting", company.get("industry"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void unknownDomainGetsDefault() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 4, "email", "user@unknown.org"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> company = (Map<String, Object>) enriched.get(0).get("company");
        assertEquals("Unknown", company.get("company"));
        assertEquals("Unknown", company.get("industry"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@acme.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals("Alice", enriched.get(0).get("name"));
        assertEquals(1, enriched.get(0).get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesRecordWithNoEmail() {
        List<Map<String, Object>> records = List.of(Map.of("id", 5, "name", "NoEmail"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> company = (Map<String, Object>) enriched.get(0).get("company");
        assertEquals("Unknown", company.get("company"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enriched"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesMultipleRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "a@acme.com"),
                Map.of("id", 2, "email", "b@globex.com"),
                Map.of("id", 3, "email", "c@initech.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals(3, enriched.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
