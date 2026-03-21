package dataversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DvrStoreWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dvr_store";
    }

    @Override
    public TaskResult execute(Task task) {
        String datasetId = (String) task.getInputData().getOrDefault("datasetId", "ds");
        String tagName = (String) task.getInputData().getOrDefault("tagName", "v1");
        System.out.println("  [store] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stored", true);
        result.getOutputData().put("storagePath", "s3://data-versions/" + datasetId + "/" + tagName);
        return result;
    }
}