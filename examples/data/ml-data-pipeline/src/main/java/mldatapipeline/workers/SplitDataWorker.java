package mldatapipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class SplitDataWorker implements Worker {
    @Override public String getTaskDefName() { return "ml_split_data"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("cleanData");
        if (data == null) data = List.of();

        Object ratioObj = task.getInputData().get("splitRatio");
        double ratio = 0.8;
        if (ratioObj instanceof Number) ratio = ((Number) ratioObj).doubleValue();

        int splitIdx = (int) Math.floor(data.size() * ratio);
        List<Map<String, Object>> trainData = data.subList(0, splitIdx);
        List<Map<String, Object>> testData = data.subList(splitIdx, data.size());

        System.out.println("  [split] Split " + data.size() + " records: " + trainData.size() + " train, " + testData.size() + " test");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trainData", new ArrayList<>(trainData));
        result.getOutputData().put("testData", new ArrayList<>(testData));
        result.getOutputData().put("trainSize", trainData.size());
        result.getOutputData().put("testSize", testData.size());
        return result;
    }
}
