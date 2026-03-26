package hronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates an employee profile during onboarding.
 */
public class CreateProfileWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hro_create_profile";
    }

    @Override
    public TaskResult execute(Task task) {
        String employeeName = (String) task.getInputData().get("employeeName");
        String department = (String) task.getInputData().get("department");

        System.out.println("  [profile] Created profile for " + employeeName + " in " + department);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("employeeId", "EMP-605");
        result.getOutputData().put("email", "jdoe@company.com");
        return result;
    }
}
