package datalakeingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateCatalogWorker implements Worker {
    @Override public String getTaskDefName() { return "li_update_catalog"; }

    @Override
    public TaskResult execute(Task task) {
        String lakePath = (String) task.getInputData().get("lakePath");
        if (lakePath == null) lakePath = "s3://data-lake";
        Object filesWrittenObj = task.getInputData().get("filesWritten");
        int filesWritten = filesWrittenObj instanceof Number ? ((Number) filesWrittenObj).intValue() : 0;
        Object totalRecordsObj = task.getInputData().get("totalRecords");
        int totalRecords = totalRecordsObj instanceof Number ? ((Number) totalRecordsObj).intValue() : 0;

        String summary = "Ingested " + totalRecords + " records into " + lakePath + " (" + filesWritten + " files)";
        System.out.println("  [catalog] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updated", true);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
