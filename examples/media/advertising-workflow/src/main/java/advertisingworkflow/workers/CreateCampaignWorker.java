package advertisingworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CreateCampaignWorker implements Worker {
    @Override public String getTaskDefName() { return "adv_create_campaign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] Processing " + task.getInputData().getOrDefault("creativeId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("creativeId", "CRE-530-001");
        r.getOutputData().put("adFormats", List.of("banner_300x250"));
        r.getOutputData().put("createdAt", "2026-03-08T08:00:00Z");
        return r;
    }
}
