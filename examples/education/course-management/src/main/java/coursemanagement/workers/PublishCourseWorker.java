package coursemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publishes the course to the catalog.
 * Input: courseId, schedule, instructor
 * Output: published, enrollmentOpen
 */
public class PublishCourseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crs_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String courseId = (String) task.getInputData().get("courseId");

        System.out.println("  [publish] " + courseId + " published to course catalog");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("enrollmentOpen", true);
        return result;
    }
}
