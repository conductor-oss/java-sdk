package mldatapipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "ml_collect_data"; }

    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("features", List.of(5.1, 3.5, 1.4, 0.2), "label", "setosa"));
        data.add(Map.of("features", List.of(4.9, 3.0, 1.4, 0.2), "label", "setosa"));
        data.add(Map.of("features", List.of(7.0, 3.2, 4.7, 1.4), "label", "versicolor"));
        data.add(Map.of("features", List.of(6.4, 3.2, 4.5, 1.5), "label", "versicolor"));
        data.add(Map.of("features", List.of(6.3, 3.3, 6.0, 2.5), "label", "virginica"));
        data.add(Map.of("features", List.of(5.8, 2.7, 5.1, 1.9), "label", "virginica"));

        // Record with null feature
        Map<String, Object> nullRecord = new HashMap<>();
        List<Object> nullFeatures = new ArrayList<>();
        nullFeatures.add(null);
        nullFeatures.add(3.0);
        nullFeatures.add(1.5);
        nullFeatures.add(0.2);
        nullRecord.put("features", nullFeatures);
        nullRecord.put("label", "setosa");
        data.add(nullRecord);

        data.add(Map.of("features", List.of(6.7, 3.1, 4.4, 1.4), "label", "versicolor"));
        data.add(Map.of("features", List.of(7.7, 2.6, 6.9, 2.3), "label", "virginica"));
        data.add(Map.of("features", List.of(5.0, 3.4, 1.5, 0.2), "label", "setosa"));

        System.out.println("  [collect] Collected " + data.size() + " records from iris_dataset");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", data);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
