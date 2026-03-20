package gdprconsent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;

public class PresentOptionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gdc_present_options";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        System.out.println("  [present] Consent options presented to " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("options", List.of("analytics", "marketing", "thirdParty", "cookies"));
        result.getOutputData().put("presentedAt", Instant.now().toString());
        return result;
    }
}
