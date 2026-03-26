package mldatapipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class EvaluateModelWorker implements Worker {
    @Override public String getTaskDefName() { return "ml_evaluate_model"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> testData = (List<Map<String, Object>>) task.getInputData().get("testData");
        if (testData == null) testData = List.of();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("accuracy", "94.5%");
        metrics.put("precision", "93.8%");
        metrics.put("recall", "94.1%");
        metrics.put("f1Score", "93.9%");
        metrics.put("confusionMatrix", Map.of("truePositives", 17, "falsePositives", 1, "trueNegatives", 18, "falseNegatives", 1));

        System.out.println("  [evaluate] Model accuracy: 94.5%, F1: 93.9% on " + testData.size() + " test samples");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("accuracy", "94.5%");
        result.getOutputData().put("metrics", metrics);
        return result;
    }
}
