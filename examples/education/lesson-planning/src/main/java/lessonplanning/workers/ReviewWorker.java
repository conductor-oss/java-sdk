package lessonplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "lpl_review"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [review] Lesson plan reviewed and approved by department");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "approved");
        result.getOutputData().put("feedback", "Well structured");
        return result;
    }
}
