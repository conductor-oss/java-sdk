package batchmltraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BmlPrepareDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bml_prepare_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String datasetId = (String) task.getInputData().getOrDefault("datasetId", "unknown");
        System.out.println("  [prepare] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dataPath", "/data/" + datasetId + "/prepared");
        result.getOutputData().put("samples", 50000);
        result.getOutputData().put("features", 128);
        return result;
    }
}