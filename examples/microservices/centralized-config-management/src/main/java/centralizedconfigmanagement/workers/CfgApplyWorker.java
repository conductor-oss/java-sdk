package centralizedconfigmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CfgApplyWorker implements Worker {
    @Override public String getTaskDefName() { return "cfg_apply_config"; }
    @Override public TaskResult execute(Task task) {
        String key = (String) task.getInputData().getOrDefault("configKey", "unknown");
        System.out.println("  [apply] Rolling out " + key + " across services");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("appliedServices", List.of("svc-a", "svc-b", "svc-c"));
        r.getOutputData().put("version", 5);
        return r;
    }
}
