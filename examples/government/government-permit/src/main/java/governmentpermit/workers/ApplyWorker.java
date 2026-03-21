package governmentpermit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gvp_apply";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        String permitType = (String) task.getInputData().get("permitType");
        System.out.printf("  [apply] Permit application from %s for %s%n", applicantId, permitType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("application", Map.of(
                "id", "PRM-001",
                "applicantId", applicantId,
                "permitType", permitType
        ));
        return result;
    }
}
