package prescriptionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Tracks the prescription for refills and adherence monitoring.
 * Input: prescriptionId, patientId, dispensedAt
 * Output: trackingId, refillDate, adherenceMonitoring
 */
public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prx_track";
    }

    @Override
    public TaskResult execute(Task task) {
        String prescriptionId = (String) task.getInputData().get("prescriptionId");
        if (prescriptionId == null) prescriptionId = "UNKNOWN";

        System.out.println("  [track] Tracking prescription " + prescriptionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trackingId", "TRK-PRX-44201");
        result.getOutputData().put("refillDate", "2024-04-15");
        result.getOutputData().put("adherenceMonitoring", true);
        return result;
    }
}
