package imageprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizeWorkerTest {

    private final OptimizeWorker worker = new OptimizeWorker();

    @Test void taskDefName() { assertEquals("ip_optimize", worker.getTaskDefName()); }

    @Test void optimizesImage() {
        TaskResult r = worker.execute(taskWith(Map.of("format", "png")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("optimized", r.getOutputData().get("result"));
    }

    @Test void returnsOptimizedSize() {
        TaskResult r = worker.execute(taskWith(Map.of("format", "png")));
        assertEquals("1.8MB", r.getOutputData().get("optimizedSize"));
    }

    @Test void returnsCompressionRatio() {
        TaskResult r = worker.execute(taskWith(Map.of("format", "jpg")));
        assertEquals(0.57, r.getOutputData().get("compressionRatio"));
    }

    @Test void preservesFormat() {
        TaskResult r = worker.execute(taskWith(Map.of("format", "webp")));
        assertEquals("webp", r.getOutputData().get("format"));
    }

    @Test void handlesNullFormat() {
        Map<String, Object> input = new HashMap<>(); input.put("format", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals("png", r.getOutputData().get("format"));
    }

    @Test void handlesMissingFormat() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals("png", r.getOutputData().get("format"));
    }

    @Test void handlesEmptyFormat() {
        TaskResult r = worker.execute(taskWith(Map.of("format", "")));
        assertEquals("png", r.getOutputData().get("format"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
