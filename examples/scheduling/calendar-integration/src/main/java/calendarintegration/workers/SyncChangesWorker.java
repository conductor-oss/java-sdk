package calendarintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class SyncChangesWorker implements Worker {
    @Override public String getTaskDefName() { return "cal_sync_changes"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        int additions = 0; try { additions = Integer.parseInt(String.valueOf(task.getInputData().get("additions"))); } catch (Exception ignored) {}
        int updates = 0; try { updates = Integer.parseInt(String.valueOf(task.getInputData().get("updates"))); } catch (Exception ignored) {}
        int deletions = 0; try { deletions = Integer.parseInt(String.valueOf(task.getInputData().get("deletions"))); } catch (Exception ignored) {}
        int total = additions + updates + deletions;
        System.out.println("  [sync] Syncing " + total + " changes");
        result.getOutputData().put("changesSynced", total);
        result.getOutputData().put("syncedAt", "2026-03-08T06:00:00Z");
        return result;
    }
}
