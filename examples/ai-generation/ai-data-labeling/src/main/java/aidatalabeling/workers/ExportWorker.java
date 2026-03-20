package aidatalabeling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "adl_export";
    }

    @Override
    public TaskResult execute(Task task) {
        Object reconciledLabels = task.getInputData().get("reconciledLabels");
        String datasetId = (String) task.getInputData().get("datasetId");
        System.out.printf("  [export] %s labeled samples exported for dataset %s%n", reconciledLabels, datasetId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("exportPath", "/datasets/DS-ai-data-labeling-labeled.json");
        result.getOutputData().put("format", "COCO");
        result.getOutputData().put("exported", true);
        return result;
    }
}
