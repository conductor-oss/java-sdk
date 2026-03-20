package imageprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadImageWorkerTest {

    private final LoadImageWorker worker = new LoadImageWorker();

    @Test void taskDefName() { assertEquals("ip_load_image", worker.getTaskDefName()); }

    @Test void loadsImage() {
        Task task = taskWith(Map.of("imageUrl", "https://example.com/photo.png"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("base64_deterministic_data", r.getOutputData().get("imageData"));
    }

    @Test void returnsExpectedDimensions() {
        TaskResult r = worker.execute(taskWith(Map.of("imageUrl", "test.png")));
        assertEquals(3840, r.getOutputData().get("width"));
        assertEquals(2160, r.getOutputData().get("height"));
    }

    @Test void returnsOriginalSize() {
        TaskResult r = worker.execute(taskWith(Map.of("imageUrl", "test.png")));
        assertEquals("4.2MB", r.getOutputData().get("originalSize"));
    }

    @Test void returnsFormat() {
        TaskResult r = worker.execute(taskWith(Map.of("imageUrl", "test.png")));
        assertEquals("png", r.getOutputData().get("format"));
    }

    @Test void handlesNullUrl() {
        Map<String, Object> input = new HashMap<>(); input.put("imageUrl", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingUrl() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesEmptyUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("imageUrl", "")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
