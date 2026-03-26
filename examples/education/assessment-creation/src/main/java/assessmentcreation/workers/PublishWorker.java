package assessmentcreation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "asc_publish"; }

    @Override
    public TaskResult execute(Task task) {
        String assessmentId = (String) task.getInputData().get("assessmentId");
        String courseId = (String) task.getInputData().get("courseId");
        System.out.println("  [publish] " + assessmentId + " published for " + courseId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("availableDate", "2024-05-10");
        return result;
    }
}
