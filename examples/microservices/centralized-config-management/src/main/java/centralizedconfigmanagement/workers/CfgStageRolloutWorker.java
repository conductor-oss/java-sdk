package centralizedconfigmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CfgStageRolloutWorker implements Worker {
    @Override public String getTaskDefName() { return "cfg_stage_rollout"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [stage] Planning rollout to services");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("plan", Map.of("stages", List.of("canary", "25%", "100%"), "interval", "5m"));
        return r;
    }
}
