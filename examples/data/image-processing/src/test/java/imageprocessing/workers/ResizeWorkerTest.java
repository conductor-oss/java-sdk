package imageprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResizeWorkerTest {

    private final ResizeWorker worker = new ResizeWorker();

    @Test void taskDefName() { assertEquals("ip_resize", worker.getTaskDefName()); }

    @SuppressWarnings("unchecked")
    @Test void resizesToGivenSizes() {
        List<Map<String, Object>> sizes = List.of(Map.of("w", 800, "h", 600), Map.of("w", 400, "h", 300));
        TaskResult r = worker.execute(taskWith(Map.of("sizes", sizes)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        List<Map<String, Object>> variants = (List<Map<String, Object>>) r.getOutputData().get("variants");
        assertEquals(2, variants.size());
        assertEquals(800, variants.get(0).get("width"));
        assertEquals(600, variants.get(0).get("height"));
    }

    @Test void variantCountMatchesList() {
        List<Map<String, Object>> sizes = List.of(Map.of("w", 100, "h", 100));
        TaskResult r = worker.execute(taskWith(Map.of("sizes", sizes)));
        assertEquals(1, r.getOutputData().get("variantCount"));
    }

    @SuppressWarnings("unchecked")
    @Test void defaultSizesWhenNull() {
        Map<String, Object> input = new HashMap<>(); input.put("sizes", null);
        TaskResult r = worker.execute(taskWith(input));
        List<?> variants = (List<?>) r.getOutputData().get("variants");
        assertEquals(3, variants.size());
    }

    @SuppressWarnings("unchecked")
    @Test void defaultSizesWhenMissing() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        List<?> variants = (List<?>) r.getOutputData().get("variants");
        assertEquals(3, variants.size());
    }

    @SuppressWarnings("unchecked")
    @Test void variantContainsSize() {
        List<Map<String, Object>> sizes = List.of(Map.of("w", 1920, "h", 1080));
        TaskResult r = worker.execute(taskWith(Map.of("sizes", sizes)));
        List<Map<String, Object>> variants = (List<Map<String, Object>>) r.getOutputData().get("variants");
        assertNotNull(variants.get(0).get("size"));
    }

    @SuppressWarnings("unchecked")
    @Test void sizeStringEndsWithKB() {
        List<Map<String, Object>> sizes = List.of(Map.of("w", 800, "h", 600));
        TaskResult r = worker.execute(taskWith(Map.of("sizes", sizes)));
        List<Map<String, Object>> variants = (List<Map<String, Object>>) r.getOutputData().get("variants");
        assertTrue(((String) variants.get(0).get("size")).endsWith("KB"));
    }

    @Test void handlesEmptySizes() {
        TaskResult r = worker.execute(taskWith(Map.of("sizes", List.of())));
        List<?> variants = (List<?>) r.getOutputData().get("variants");
        assertEquals(3, variants.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
