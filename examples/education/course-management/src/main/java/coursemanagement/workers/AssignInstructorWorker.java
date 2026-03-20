package coursemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assigns an instructor to the course.
 * Input: courseId, department
 * Output: instructor
 */
public class AssignInstructorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crs_assign_instructor";
    }

    @Override
    public TaskResult execute(Task task) {
        String courseId = (String) task.getInputData().get("courseId");
        String instructor = "Dr. Sarah Chen";

        System.out.println("  [assign] " + courseId + " assigned to " + instructor);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("instructor", instructor);
        return result;
    }
}
