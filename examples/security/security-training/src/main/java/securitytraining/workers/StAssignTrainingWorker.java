package securitytraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assigns security training modules to department employees.
 * Input: department, trainingModule
 * Output: assign_trainingId, success
 */
public class StAssignTrainingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_assign_training";
    }

    @Override
    public TaskResult execute(Task task) {
        String trainingModule = (String) task.getInputData().get("trainingModule");
        if (trainingModule == null) trainingModule = "general-awareness";

        String department = (String) task.getInputData().get("department");
        if (department == null) department = "all";

        System.out.println("  [assign] " + trainingModule + " assigned to " + department + ": 45 employees");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assign_trainingId", "ASSIGN_TRAINING-1393");
        result.getOutputData().put("success", true);
        return result;
    }
}
