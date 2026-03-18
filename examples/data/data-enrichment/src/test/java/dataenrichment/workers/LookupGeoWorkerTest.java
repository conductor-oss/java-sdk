package dataenrichment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupGeoWorkerTest {

    private final LookupGeoWorker worker = new LookupGeoWorker();

    @Test
    void taskDefName() {
        assertEquals("dr_lookup_geo", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesSanFranciscoZip() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "zip", "94105"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals(1, enriched.size());
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("San Francisco", geo.get("city"));
        assertEquals("CA", geo.get("state"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesNewYorkZip() {
        List<Map<String, Object>> records = List.of(Map.of("id", 2, "zip", "10001"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("New York", geo.get("city"));
        assertEquals("NY", geo.get("state"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesChicagoZip() {
        List<Map<String, Object>> records = List.of(Map.of("id", 3, "zip", "60601"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("Chicago", geo.get("city"));
        assertEquals("IL", geo.get("state"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void unknownZipGetsDefault() {
        List<Map<String, Object>> records = List.of(Map.of("id", 4, "zip", "99999"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("Unknown", geo.get("city"));
        assertEquals("Unknown", geo.get("state"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "zip", "94105"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals("Alice", enriched.get(0).get("name"));
        assertEquals(1, enriched.get(0).get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesMultipleRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "zip", "94105"),
                Map.of("id", 2, "zip", "10001"),
                Map.of("id", 3, "zip", "60601"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals(3, enriched.size());
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
    void handlesRecordWithNoZip() {
        List<Map<String, Object>> records = List.of(Map.of("id", 5, "name", "NoZip"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("Unknown", geo.get("city"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void performsRealDnsLookupForIp() {
        // Use loopback IP which always resolves
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "ip", "127.0.0.1"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> dns = (Map<String, Object>) enriched.get(0).get("dns");
        assertNotNull(dns);
        assertEquals("127.0.0.1", dns.get("ip"));
        assertNotNull(dns.get("hostname"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void performsRealDnsLookupForHostname() {
        // localhost always resolves
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "hostname", "localhost"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> dns = (Map<String, Object>) enriched.get(0).get("dns");
        assertNotNull(dns);
        assertEquals("localhost", dns.get("hostname"));
        assertNotNull(dns.get("ip"));
        assertTrue((Boolean) dns.get("resolved"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesBeverlyHillsZip() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "zip", "90210"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> geo = (Map<String, Object>) enriched.get(0).get("geo");
        assertEquals("Beverly Hills", geo.get("city"));
        assertEquals("CA", geo.get("state"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
