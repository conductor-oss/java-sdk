package fanoutfanin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessImageWorkerTest {

    private final ProcessImageWorker worker = new ProcessImageWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_process_image", worker.getTaskDefName());
    }

    @Test
    void processesImageWithDeterministicSize() {
        Task task = taskWith(Map.of(
                "image", Map.of("name", "hero.jpg", "size", 2400),
                "index", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hero.jpg", result.getOutputData().get("name"));
        assertEquals(2400, result.getOutputData().get("originalSize"));
        assertEquals(800, result.getOutputData().get("processedSize")); // 2400 / 3
        assertEquals("webp", result.getOutputData().get("format"));
        assertNotNull(result.getOutputData().get("processingTime"));
    }

    @Test
    void processedSizeIsOriginalDividedByThree() {
        Task task = taskWith(Map.of(
                "image", Map.of("name", "banner.png", "size", 3600),
                "index", 1
        ));
        TaskResult result = worker.execute(task);

        assertEquals(3600, result.getOutputData().get("originalSize"));
        assertEquals(1200, result.getOutputData().get("processedSize")); // 3600 / 3
    }

    @Test
    void handlesSmallImage() {
        Task task = taskWith(Map.of(
                "image", Map.of("name", "icon.png", "size", 5),
                "index", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("originalSize"));
        assertEquals(1, result.getOutputData().get("processedSize")); // 5 / 3 = 1 (integer division)
    }

    @Test
    void handlesNullImage() {
        Task task = taskWith(new HashMap<>(Map.of("index", 0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("name"));
        assertEquals(0, result.getOutputData().get("originalSize"));
        assertEquals(0, result.getOutputData().get("processedSize"));
        assertEquals("webp", result.getOutputData().get("format"));
    }

    @Test
    void handlesNullIndex() {
        Task task = taskWith(new HashMap<>(Map.of(
                "image", Map.of("name", "test.jpg", "size", 900)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("test.jpg", result.getOutputData().get("name"));
        assertEquals(300, result.getOutputData().get("processedSize")); // 900 / 3
    }

    @Test
    void outputIsDeterministic() {
        Map<String, Object> input = Map.of(
                "image", Map.of("name", "test.jpg", "size", 1500),
                "index", 2
        );
        Task task1 = taskWith(input);
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(input);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData(), result2.getOutputData());
    }

    @Test
    void alwaysReturnsWebpFormat() {
        Task task = taskWith(Map.of(
                "image", Map.of("name", "photo.png", "size", 3000),
                "index", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals("webp", result.getOutputData().get("format"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
