package fanoutfanin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {

    private final AggregateWorker worker = new AggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_aggregate", worker.getTaskDefName());
    }

    @Test
    void aggregatesMultipleResults() {
        Map<String, Object> joinOutput = Map.of(
                "img_0_ref", Map.of(
                        "name", "hero.jpg",
                        "originalSize", 2400,
                        "processedSize", 800,
                        "format", "webp",
                        "processingTime", 90
                ),
                "img_1_ref", Map.of(
                        "name", "banner.png",
                        "originalSize", 3600,
                        "processedSize", 1200,
                        "format", "webp",
                        "processingTime", 110
                )
        );
        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("processedCount"));
        assertEquals(6000, result.getOutputData().get("totalOriginal"));
        assertEquals(2000, result.getOutputData().get("totalProcessed"));

        // savings = (1 - 2000/6000) * 100 = 66.67
        double savings = ((Number) result.getOutputData().get("savings")).doubleValue();
        assertEquals(66.67, savings, 0.01);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify sorted order (img_0_ref before img_1_ref)
        assertEquals("hero.jpg", results.get(0).get("name"));
        assertEquals("banner.png", results.get(1).get("name"));
    }

    @Test
    void calculatesSavingsCorrectly() {
        // With processedSize = originalSize / 3, savings should be ~66.67%
        Map<String, Object> joinOutput = Map.of(
                "img_0_ref", Map.of(
                        "name", "test.jpg",
                        "originalSize", 3000,
                        "processedSize", 1000,
                        "format", "webp",
                        "processingTime", 50
                )
        );
        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        double savings = ((Number) result.getOutputData().get("savings")).doubleValue();
        assertEquals(66.67, savings, 0.01);
    }

    @Test
    void handlesEmptyJoinOutput() {
        Task task = taskWith(Map.of("joinOutput", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
        assertEquals(0, result.getOutputData().get("totalOriginal"));
        assertEquals(0, result.getOutputData().get("totalProcessed"));
        assertEquals(0.0, ((Number) result.getOutputData().get("savings")).doubleValue());
    }

    @Test
    void handlesNullJoinOutput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
        assertEquals(0, result.getOutputData().get("totalOriginal"));
        assertEquals(0, result.getOutputData().get("totalProcessed"));
    }

    @Test
    void ignoresNonImageKeys() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("img_0_ref", Map.of(
                "name", "hero.jpg",
                "originalSize", 2400,
                "processedSize", 800,
                "format", "webp",
                "processingTime", 90
        ));
        joinOutput.put("some_other_task_ref", Map.of(
                "data", "irrelevant"
        ));
        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("processedCount"));
        assertEquals(2400, result.getOutputData().get("totalOriginal"));
    }

    @Test
    void resultsAreOrderedByIndex() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("img_2_ref", Map.of(
                "name", "c.jpg", "originalSize", 300, "processedSize", 100,
                "format", "webp", "processingTime", 30
        ));
        joinOutput.put("img_0_ref", Map.of(
                "name", "a.jpg", "originalSize", 100, "processedSize", 33,
                "format", "webp", "processingTime", 10
        ));
        joinOutput.put("img_1_ref", Map.of(
                "name", "b.jpg", "originalSize", 200, "processedSize", 66,
                "format", "webp", "processingTime", 20
        ));
        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(3, results.size());
        // TreeMap sorts keys lexicographically: img_0_ref, img_1_ref, img_2_ref
        assertEquals("a.jpg", results.get(0).get("name"));
        assertEquals("b.jpg", results.get(1).get("name"));
        assertEquals("c.jpg", results.get(2).get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
