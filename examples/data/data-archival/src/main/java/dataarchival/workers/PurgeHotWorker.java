package dataarchival.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Purges stale records from hot storage if archive is verified.
 * Input: staleRecordIds (list), archiveVerified (boolean)
 * Output: purgedCount (int), summary (string), skipped (boolean)
 */
public class PurgeHotWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "arc_purge_hot";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Object> ids = (List<Object>) task.getInputData().get("staleRecordIds");
        if (ids == null) {
            ids = List.of();
        }
        Object verifiedObj = task.getInputData().get("archiveVerified");
        boolean verified = Boolean.TRUE.equals(verifiedObj);

        int purgedCount = 0;
        if (verified) {
            purgedCount = ids.size();
            System.out.println("  [purge] Purged " + purgedCount + " stale records from hot storage (archive verified)");
        } else {
            System.out.println("  [purge] SKIPPED purge — archive not verified");
        }

        String summary = "Archival complete: " + purgedCount + " records archived and purged from hot storage";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("purgedCount", purgedCount);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("skipped", !verified);
        return result;
    }
}
