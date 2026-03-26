package hronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assigns a mentor from the same department to the new employee.
 */
public class AssignMentorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hro_assign_mentor";
    }

    @Override
    public TaskResult execute(Task task) {
        String department = (String) task.getInputData().get("department");

        System.out.println("  [mentor] Assigned mentor from " + department + " department");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mentorId", "EMP-150");
        result.getOutputData().put("mentorName", "Sarah Chen");
        return result;
    }
}
