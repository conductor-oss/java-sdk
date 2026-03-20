package aidatalabeling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class PrepareDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "adl_prepare_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String datasetId = (String) task.getInputData().get("datasetId");
        String labelType = (String) task.getInputData().get("labelType");
        System.out.printf("  [prepare] Dataset %s prepared — 500 samples for %s%n", datasetId, labelType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batch", Map.of("sampleCount", 500, "type", labelType));
        return result;
    }
}
