package lessonplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "lpl_publish"; }

    @Override
    public TaskResult execute(Task task) {
        String courseId = (String) task.getInputData().get("courseId");
        Object week = task.getInputData().get("week");
        System.out.println("  [publish] Lesson published for " + courseId + ", week " + week);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("visibleToStudents", true);
        return result;
    }
}
