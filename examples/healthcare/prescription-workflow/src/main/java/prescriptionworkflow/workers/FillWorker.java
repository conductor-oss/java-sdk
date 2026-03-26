package prescriptionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fills the prescription order.
 * Input: prescriptionId, medication, dosage, interactionsCleared
 * Output: fillId, quantity, daysSupply
 */
public class FillWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prx_fill";
    }

    @Override
    public TaskResult execute(Task task) {
        String medication = (String) task.getInputData().get("medication");
        String dosage = (String) task.getInputData().get("dosage");
        if (medication == null) medication = "UNKNOWN";
        if (dosage == null) dosage = "UNKNOWN";

        System.out.println("  [fill] Filling " + medication + " " + dosage);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fillId", "FILL-78901");
        result.getOutputData().put("quantity", 30);
        result.getOutputData().put("daysSupply", 30);
        return result;
    }
}
