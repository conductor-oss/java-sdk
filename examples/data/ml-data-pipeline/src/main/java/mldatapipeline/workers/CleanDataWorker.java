package mldatapipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CleanDataWorker implements Worker {
    @Override public String getTaskDefName() { return "ml_clean_data"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> rawData = (List<Map<String, Object>>) task.getInputData().get("rawData");
        if (rawData == null) rawData = List.of();

        List<Map<String, Object>> clean = new ArrayList<>();
        for (Map<String, Object> record : rawData) {
            List<Object> features = (List<Object>) record.get("features");
            if (features != null && features.stream().allMatch(f -> f != null)) {
                clean.add(record);
            }
        }

        int removed = rawData.size() - clean.size();
        System.out.println("  [clean] Cleaned data: " + clean.size() + " valid, " + removed + " removed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleanData", clean);
        result.getOutputData().put("cleanCount", clean.size());
        result.getOutputData().put("removedCount", removed);
        return result;
    }
}
