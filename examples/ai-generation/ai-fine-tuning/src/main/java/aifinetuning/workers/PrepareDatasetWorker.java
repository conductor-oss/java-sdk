package aifinetuning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareDatasetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aft_prepare_dataset";
    }

    @Override
    public TaskResult execute(Task task) {

        String datasetId = (String) task.getInputData().get("datasetId");
        String taskType = (String) task.getInputData().get("taskType");
        System.out.printf("  [prepare] Dataset %s prepared — 10K samples%n", datasetId, taskType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("datasetSize", 10000);
        result.getOutputData().put("datasetPath", "/data/ft-804");
        result.getOutputData().put("trainSplit", 8000);
        result.getOutputData().put("valSplit", 2000);
        return result;
    }
}
