package mldatapipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EvaluateModelWorkerTest {
    private final EvaluateModelWorker worker = new EvaluateModelWorker();

    @Test void taskDefName() { assertEquals("ml_evaluate_model", worker.getTaskDefName()); }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void returnsAccuracy() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        assertEquals("94.5%", worker.execute(task).getOutputData().get("accuracy"));
    }

    @Test @SuppressWarnings("unchecked")
    void metricsContainF1Score() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        Map<String, Object> metrics = (Map<String, Object>) worker.execute(task).getOutputData().get("metrics");
        assertEquals("93.9%", metrics.get("f1Score"));
    }

    @Test @SuppressWarnings("unchecked")
    void metricsContainConfusionMatrix() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        Map<String, Object> metrics = (Map<String, Object>) worker.execute(task).getOutputData().get("metrics");
        assertNotNull(metrics.get("confusionMatrix"));
    }

    @Test @SuppressWarnings("unchecked")
    void metricsContainPrecisionAndRecall() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        Map<String, Object> metrics = (Map<String, Object>) worker.execute(task).getOutputData().get("metrics");
        assertEquals("93.8%", metrics.get("precision"));
        assertEquals("94.1%", metrics.get("recall"));
    }

    @Test void handlesNullTestData() {
        Map<String, Object> input = new HashMap<>(); input.put("testData", null);
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void metricsNotNull() {
        Task task = taskWith(Map.of("testData", List.of(), "model", Map.of()));
        assertNotNull(worker.execute(task).getOutputData().get("metrics"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
