package selfcorrection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Diagnoses errors found during testing.
 * Returns a deterministic diagnosis identifying the missing negative-number guard.
 */
public class DiagnoseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sc_diagnose";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        if (code == null || code.isBlank()) {
            code = "";
        }

        List<String> errors = (List<String>) task.getInputData().get("errors");
        if (errors == null) {
            errors = List.of();
        }

        System.out.println("  [sc_diagnose] Diagnosing " + errors.size() + " error(s)...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diagnosis",
                "Missing guard for negative numbers — function recurses infinitely for n < 0");
        result.getOutputData().put("severity", "high");
        return result;
    }
}
