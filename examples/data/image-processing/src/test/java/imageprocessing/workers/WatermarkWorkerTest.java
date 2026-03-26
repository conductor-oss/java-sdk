package imageprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WatermarkWorkerTest {

    private final WatermarkWorker worker = new WatermarkWorker();

    @Test void taskDefName() { assertEquals("ip_watermark", worker.getTaskDefName()); }

    @Test void appliesWatermark() {
        TaskResult r = worker.execute(taskWith(Map.of("text", "Copyright 2024")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("watermarked", r.getOutputData().get("result"));
        assertEquals(true, r.getOutputData().get("applied"));
    }

    @Test void positionIsBottomRight() {
        TaskResult r = worker.execute(taskWith(Map.of("text", "Test")));
        assertEquals("bottom-right", r.getOutputData().get("position"));
    }

    @Test void opacityIsHalf() {
        TaskResult r = worker.execute(taskWith(Map.of("text", "Test")));
        assertEquals(0.5, r.getOutputData().get("opacity"));
    }

    @Test void handlesNullText() {
        Map<String, Object> input = new HashMap<>(); input.put("text", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingText() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("applied"));
    }

    @Test void handlesEmptyText() {
        TaskResult r = worker.execute(taskWith(Map.of("text", "")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void resultIsAlwaysWatermarked() {
        TaskResult r = worker.execute(taskWith(Map.of("text", "any")));
        assertEquals("watermarked", r.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
