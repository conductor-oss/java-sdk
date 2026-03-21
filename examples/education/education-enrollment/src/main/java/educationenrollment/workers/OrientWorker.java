package educationenrollment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Schedules orientation for a newly enrolled student.
 * Input: studentId
 * Output: orientationDate, location
 */
public class OrientWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edu_orient";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");

        System.out.println("  [orient] Orientation scheduled for " + studentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orientationDate", "2024-08-20");
        result.getOutputData().put("location", "Main Campus");
        return result;
    }
}
