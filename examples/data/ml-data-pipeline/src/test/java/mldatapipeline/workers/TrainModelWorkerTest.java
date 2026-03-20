package mldatapipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainModelWorkerTest {
    private final TrainModelWorker worker = new TrainModelWorker();

    @Test void taskDefName() { assertEquals("ml_train_model", worker.getTaskDefName()); }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("trainData", List.of(Map.of("label", "a")), "modelType", "random_forest"));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test @SuppressWarnings("unchecked")
    void modelContainsType() {
        Task task = taskWith(Map.of("trainData", List.of(Map.of("label", "a")), "modelType", "svm"));
        TaskResult r = worker.execute(task);
        Map<String, Object> model = (Map<String, Object>) r.getOutputData().get("model");
        assertEquals("svm", model.get("type"));
    }

    @Test @SuppressWarnings("unchecked")
    void modelContainsClasses() {
        Task task = taskWith(Map.of("trainData", List.of(Map.of("label", "a"), Map.of("label", "b"), Map.of("label", "a")), "modelType", "rf"));
        TaskResult r = worker.execute(task);
        Map<String, Object> model = (Map<String, Object>) r.getOutputData().get("model");
        List<String> classes = (List<String>) model.get("classes");
        assertEquals(2, classes.size());
    }

    @Test @SuppressWarnings("unchecked")
    void modelContainsTrainedCount() {
        List<Map<String, Object>> data = List.of(Map.of("label", "a"), Map.of("label", "b"));
        Task task = taskWith(Map.of("trainData", data, "modelType", "rf"));
        TaskResult r = worker.execute(task);
        Map<String, Object> model = (Map<String, Object>) r.getOutputData().get("model");
        assertEquals(2, model.get("trainedOn"));
    }

    @Test void defaultsToRandomForest() {
        Task task = taskWith(Map.of("trainData", List.of(Map.of("label", "a"))));
        TaskResult r = worker.execute(task);
        @SuppressWarnings("unchecked") Map<String, Object> model = (Map<String, Object>) r.getOutputData().get("model");
        assertEquals("random_forest", model.get("type"));
    }

    @Test void returnsTrainingLoss() {
        Task task = taskWith(Map.of("trainData", List.of(Map.of("label", "a")), "modelType", "rf"));
        TaskResult r = worker.execute(task);
        assertEquals(0.12, r.getOutputData().get("trainingLoss"));
    }

    @Test void handlesNullTrainData() {
        Map<String, Object> input = new HashMap<>(); input.put("trainData", null); input.put("modelType", "rf");
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
