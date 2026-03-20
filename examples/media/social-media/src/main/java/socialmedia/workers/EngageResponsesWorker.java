package socialmedia.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EngageResponsesWorker implements Worker {
    @Override public String getTaskDefName() { return "soc_engage_responses"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [engage] Processing " + task.getInputData().getOrDefault("responsesHandled", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("responsesHandled", "total");
        r.getOutputData().put("replied", 15);
        r.getOutputData().put("liked", 28);
        r.getOutputData().put("flaggedForReview", 6);
        return r;
    }
}
