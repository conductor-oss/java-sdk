package selfhealing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sh_diagnose — diagnoses problems in an unhealthy service.
 *
 * Analyzes symptoms and returns a diagnosis with recommended action.
 * Returns diagnosis="connection_pool_exhausted", action="restart_connection_pool".
 */
public class DiagnoseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_diagnose";
    }

    @Override
    public TaskResult execute(Task task) {
        String symptoms = "";
        Object symptomsInput = task.getInputData().get("symptoms");
        if (symptomsInput != null) {
            symptoms = symptomsInput.toString();
        }

        String service = "";
        Object serviceInput = task.getInputData().get("service");
        if (serviceInput != null) {
            service = serviceInput.toString();
        }

        System.out.println("  [sh_diagnose] Diagnosing service=" + service + " symptoms=" + symptoms);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diagnosis", "connection_pool_exhausted");
        result.getOutputData().put("action", "restart_connection_pool");

        System.out.println("  [sh_diagnose] Diagnosis: connection_pool_exhausted, action: restart_connection_pool");

        return result;
    }
}
