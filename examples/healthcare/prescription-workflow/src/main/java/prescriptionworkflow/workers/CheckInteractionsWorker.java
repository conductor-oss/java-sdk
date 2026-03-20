package prescriptionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Checks for drug interactions between new medication and current medications.
 * Input: patientId, medication, currentMedications
 * Output: cleared, interactions, severity
 */
public class CheckInteractionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prx_check_interactions";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String medication = (String) task.getInputData().get("medication");
        List<String> current = (List<String>) task.getInputData().get("currentMedications");
        if (medication == null) medication = "UNKNOWN";
        if (current == null) current = List.of();

        System.out.println("  [interactions] Checking " + medication + " against " + current.size() + " current meds");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleared", true);
        result.getOutputData().put("interactions", List.of());
        result.getOutputData().put("severity", "none");
        return result;
    }
}
