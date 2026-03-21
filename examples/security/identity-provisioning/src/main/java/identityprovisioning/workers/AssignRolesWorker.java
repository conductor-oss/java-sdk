package identityprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssignRolesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_assign_roles";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [roles] Assigned role: senior-engineer");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assign_roles", true);
        result.addOutputData("processed", true);
        return result;
    }
}
