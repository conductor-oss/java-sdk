package datalakeingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class WriteToLakeWorker implements Worker {
    @Override public String getTaskDefName() { return "li_write_to_lake"; }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) task.getInputData().get("files");
        if (files == null) files = List.of();
        String lakePath = (String) task.getInputData().get("lakePath");
        if (lakePath == null) lakePath = "s3://data-lake";

        int totalBytes = 0;
        for (Map<String, Object> f : files) {
            Object sb = f.get("sizeBytes");
            if (sb instanceof Number) totalBytes += ((Number) sb).intValue();
        }

        System.out.println("  [write] Wrote " + files.size() + " files (" + totalBytes + " bytes) to \"" + lakePath + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("filesWritten", files.size());
        result.getOutputData().put("totalBytes", totalBytes);
        return result;
    }
}
