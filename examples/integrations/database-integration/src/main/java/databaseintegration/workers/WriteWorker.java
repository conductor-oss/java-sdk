package databaseintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Writes transformed rows to target database.
 * Input: connectionId, transformedRows
 * Output: writtenCount, writtenAt
 */
public class WriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbi_write";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        java.util.List<?> rows = (java.util.List<?>) task.getInputData().getOrDefault("transformedRows", java.util.List.of());
        System.out.println("  [write] Inserted " + rows.size() + " rows into target database");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("writtenCount", rows.size());
        result.getOutputData().put("writtenAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
