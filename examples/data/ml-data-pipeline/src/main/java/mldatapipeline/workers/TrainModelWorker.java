package mldatapipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class TrainModelWorker implements Worker {
    @Override public String getTaskDefName() { return "ml_train_model"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> trainData = (List<Map<String, Object>>) task.getInputData().get("trainData");
        if (trainData == null) trainData = List.of();
        String modelType = (String) task.getInputData().getOrDefault("modelType", "random_forest");

        Set<String> labels = new LinkedHashSet<>();
        for (Map<String, Object> r : trainData) {
            labels.add((String) r.get("label"));
        }

        System.out.println("  [train] Trained " + modelType + " on " + trainData.size() + " samples, " + labels.size() + " classes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("model", Map.of("type", modelType, "classes", new ArrayList<>(labels), "trainedOn", trainData.size()));
        result.getOutputData().put("trainingLoss", 0.12);
        return result;
    }
}
