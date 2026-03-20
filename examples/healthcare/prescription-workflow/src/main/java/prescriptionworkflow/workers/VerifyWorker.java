package prescriptionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Verifies a prescription and retrieves current medications.
 * Input: prescriptionId, patientId, medication, dosage
 * Output: verified, prescriber, currentMedications
 */
public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prx_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String prescriptionId = (String) task.getInputData().get("prescriptionId");
        String medication = (String) task.getInputData().get("medication");
        String dosage = (String) task.getInputData().get("dosage");
        if (prescriptionId == null) prescriptionId = "UNKNOWN";
        if (medication == null) medication = "UNKNOWN";
        if (dosage == null) dosage = "UNKNOWN";

        System.out.println("  [verify] Rx " + prescriptionId + ": " + medication + " " + dosage);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("prescriber", "Dr. Williams");
        result.getOutputData().put("currentMedications", List.of("Lisinopril 10mg", "Metformin 500mg"));
        return result;
    }
}
