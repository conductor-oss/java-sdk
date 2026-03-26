package prescriptionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Dispenses the filled prescription to the patient.
 * Input: prescriptionId, patientId, fillId
 * Output: dispensed, dispensedAt, pharmacist
 */
public class DispenseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prx_dispense";
    }

    @Override
    public TaskResult execute(Task task) {
        String fillId = (String) task.getInputData().get("fillId");
        String patientId = (String) task.getInputData().get("patientId");
        if (fillId == null) fillId = "UNKNOWN";
        if (patientId == null) patientId = "UNKNOWN";

        System.out.println("  [dispense] Dispensing fill " + fillId + " to patient " + patientId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dispensed", true);
        result.getOutputData().put("dispensedAt", "2024-03-15T14:30:00Z");
        result.getOutputData().put("pharmacist", "R. Patel, PharmD");
        return result;
    }
}
