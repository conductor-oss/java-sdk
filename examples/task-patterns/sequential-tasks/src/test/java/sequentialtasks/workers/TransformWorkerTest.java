package sequentialtasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformWorkerTest {

    private final TransformWorker worker = new TransformWorker();

    @Test
    void taskDefName() {
        assertEquals("seq_transform", worker.getTaskDefName());
    }

    @Test
    void transformsRawDataWithGrades() {
        List<Map<String, Object>> rawData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85),
                Map.of("id", 2, "name", "Bob", "score", 92),
                Map.of("id", 3, "name", "Carol", "score", 78)
        );

        Task task = taskWith(Map.of("rawData", rawData, "format", "enriched"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transformed =
                (List<Map<String, Object>>) result.getOutputData().get("transformedData");
        assertEquals(3, transformed.size());
        assertEquals(3, result.getOutputData().get("transformedCount"));
    }

    @Test
    void assignsCorrectGrades() {
        List<Map<String, Object>> rawData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85),
                Map.of("id", 2, "name", "Bob", "score", 92),
                Map.of("id", 3, "name", "Carol", "score", 78)
        );

        Task task = taskWith(Map.of("rawData", rawData, "format", "standard"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transformed =
                (List<Map<String, Object>>) result.getOutputData().get("transformedData");

        assertEquals("B", transformed.get(0).get("grade"));   // 85 -> B
        assertEquals("A", transformed.get(1).get("grade"));   // 92 -> A
        assertEquals("C", transformed.get(2).get("grade"));   // 78 -> C
    }

    @Test
    void calculatesNormalizedScores() {
        List<Map<String, Object>> rawData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85),
                Map.of("id", 2, "name", "Bob", "score", 92)
        );

        Task task = taskWith(Map.of("rawData", rawData, "format", "standard"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transformed =
                (List<Map<String, Object>>) result.getOutputData().get("transformedData");

        assertEquals(0.85, ((Number) transformed.get(0).get("normalizedScore")).doubleValue(), 0.001);
        assertEquals(0.92, ((Number) transformed.get(1).get("normalizedScore")).doubleValue(), 0.001);
    }

    @Test
    void preservesOriginalFields() {
        List<Map<String, Object>> rawData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85)
        );

        Task task = taskWith(Map.of("rawData", rawData, "format", "enriched"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transformed =
                (List<Map<String, Object>>) result.getOutputData().get("transformedData");

        Map<String, Object> record = transformed.get(0);
        assertEquals(1, record.get("id"));
        assertEquals("Alice", record.get("name"));
        assertEquals(85, record.get("score"));
        assertEquals("enriched", record.get("format"));
    }

    @Test
    void defaultsFormatWhenMissing() {
        List<Map<String, Object>> rawData = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85)
        );

        Task task = taskWith(Map.of("rawData", rawData));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transformed =
                (List<Map<String, Object>>) result.getOutputData().get("transformedData");

        assertEquals("standard", transformed.get(0).get("format"));
    }

    @Test
    void gradeCalculation() {
        assertEquals("A", TransformWorker.calculateGrade(90));
        assertEquals("A", TransformWorker.calculateGrade(100));
        assertEquals("B", TransformWorker.calculateGrade(80));
        assertEquals("B", TransformWorker.calculateGrade(89));
        assertEquals("C", TransformWorker.calculateGrade(79));
        assertEquals("C", TransformWorker.calculateGrade(0));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
