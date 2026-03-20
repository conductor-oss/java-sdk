package imageprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    private final FinalizeWorker worker = new FinalizeWorker();

    @Test void taskDefName() { assertEquals("ip_finalize", worker.getTaskDefName()); }

    @Test void finalizesWithVariants() {
        List<Map<String, Object>> variants = List.of(Map.of("w", 800), Map.of("w", 400), Map.of("w", 150));
        TaskResult r = worker.execute(taskWith(Map.of("resized", variants)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, r.getOutputData().get("totalOutputs"));
    }

    @Test void outputUrlIsS3() {
        TaskResult r = worker.execute(taskWith(Map.of("resized", List.of())));
        String url = (String) r.getOutputData().get("outputUrl");
        assertTrue(url.startsWith("s3://"));
    }

    @Test void totalOutputsIncludesWatermarkAndOptimized() {
        List<?> variants = List.of(Map.of("w", 100));
        TaskResult r = worker.execute(taskWith(Map.of("resized", variants)));
        assertEquals(3, r.getOutputData().get("totalOutputs"));
    }

    @Test void handlesNullResized() {
        Map<String, Object> input = new HashMap<>(); input.put("resized", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("totalOutputs"));
    }

    @Test void handlesMissingResized() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("totalOutputs"));
    }

    @Test void emptyVariantsList() {
        TaskResult r = worker.execute(taskWith(Map.of("resized", List.of())));
        assertEquals(2, r.getOutputData().get("totalOutputs"));
    }

    @Test void outputUrlNeverChanges() {
        TaskResult r1 = worker.execute(taskWith(Map.of("resized", List.of())));
        TaskResult r2 = worker.execute(taskWith(Map.of("resized", List.of(Map.of("w", 1)))));
        assertEquals(r1.getOutputData().get("outputUrl"), r2.getOutputData().get("outputUrl"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
