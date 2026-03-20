package changemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements the approved change.
 * Input: implementData (from approve output)
 * Output: implemented, completedAt, rollbackPlan
 */
public class ImplementChange implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_implement";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("implementData");
        boolean wasApproved = true;
        if (data != null && data.get("approved") != null) {
            wasApproved = Boolean.TRUE.equals(data.get("approved"));
        }

        System.out.println("[cm_implement] Change implemented successfully (approved: " + wasApproved + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("implemented", true);
        output.put("completedAt", "2026-03-14T10:00:00Z");
        output.put("rollbackPlan", "Restore previous RDS snapshot");
        result.setOutputData(output);
        return result;
    }
}
