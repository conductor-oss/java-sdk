package emailcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SendCampaignWorker implements Worker {
    @Override public String getTaskDefName() { return "eml_send_campaign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [send] Processing " + task.getInputData().getOrDefault("sendId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sendId", "SND-524-001");
        r.getOutputData().put("sentCount", task.getInputData().get("personalizedCount"));
        r.getOutputData().put("bouncedCount", 45);
        r.getOutputData().put("sentAt", "2026-03-08T10:00:00Z");
        return r;
    }
}
