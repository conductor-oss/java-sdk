package securityincident.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Triages a security incident based on type and severity.
 * Input: incidentType, severity, affectedSystem
 * Output: triageId, success
 */
public class TriageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "si_triage";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentType = (String) task.getInputData().get("incidentType");
        if (incidentType == null) {
            incidentType = "unknown";
        }
        String severity = (String) task.getInputData().get("severity");
        if (severity == null) {
            severity = "P3";
        }

        System.out.println("  [triage] " + incidentType + " -- severity: " + severity);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("triageId", "TRIAGE-1381");
        result.getOutputData().put("success", true);
        return result;
    }
}
