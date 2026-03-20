package selfhealing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sh_remediate — applies the fix recommended by diagnosis.
 *
 * Takes the diagnosis and action, applies the remediation, and returns remediated=true.
 */
public class RemediateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_remediate";
    }

    @Override
    public TaskResult execute(Task task) {
        String action = "";
        Object actionInput = task.getInputData().get("action");
        if (actionInput != null) {
            action = actionInput.toString();
        }

        String diagnosis = "";
        Object diagnosisInput = task.getInputData().get("diagnosis");
        if (diagnosisInput != null) {
            diagnosis = diagnosisInput.toString();
        }

        System.out.println("  [sh_remediate] Applying fix: " + action + " for " + diagnosis);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fixed", true);

        System.out.println("  [sh_remediate] Remediation applied successfully");

        return result;
    }
}
