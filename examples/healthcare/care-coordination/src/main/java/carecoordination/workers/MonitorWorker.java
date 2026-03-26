package carecoordination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Activates monitoring for the patient with the care team.
 * Input: patientId, careplan, team
 * Output: active, checkInFrequency, nextCheckIn
 */
public class MonitorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccr_monitor";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object teamObj = task.getInputData().get("team");
        List<?> team = teamObj instanceof List ? (List<?>) teamObj : List.of();

        System.out.println("  [monitor] Monitoring activated — " + team.size() + " team members notified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("active", true);
        result.getOutputData().put("checkInFrequency", "weekly");
        result.getOutputData().put("nextCheckIn", "2024-03-22");
        return result;
    }
}
