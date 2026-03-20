package centralizedconfigmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CfgValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "cfg_validate"; }
    @Override public TaskResult execute(Task task) {
        String key = (String) task.getInputData().getOrDefault("configKey", "unknown");
        Object val = task.getInputData().getOrDefault("configValue", "");
        System.out.println("  [validate] Config " + key + " = " + val + ": valid");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true);
        return r;
    }
}
