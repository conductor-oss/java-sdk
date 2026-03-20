package coursemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates a new course entry.
 * Input: courseName, department, credits
 * Output: courseId
 */
public class CreateCourseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crs_create";
    }

    @Override
    public TaskResult execute(Task task) {
        String courseName = (String) task.getInputData().get("courseName");
        String department = (String) task.getInputData().get("department");
        Object credits = task.getInputData().get("credits");

        System.out.println("  [create] Course: " + courseName + " (" + department + ", " + credits + " credits)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("courseId", "CS-672-101");
        return result;
    }
}
