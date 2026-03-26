package trainingmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssessWorker implements Worker {
    @Override public String getTaskDefName() { return "trm_assess"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assess] Assessment completed: score 92/100");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("score", 92);
        r.getOutputData().put("passed", true);
        r.getOutputData().put("passingScore", 80);
        return r;
    }
}
