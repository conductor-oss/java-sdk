package nonprofitdonation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ThankYouWorker implements Worker {
    @Override public String getTaskDefName() { return "don_thank_you"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [thanks] Thank you sent to " + task.getInputData().get("donorName") + " for " + task.getInputData().get("campaign"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("thanked", true); r.addOutputData("channel", "email");
        return r;
    }
}
