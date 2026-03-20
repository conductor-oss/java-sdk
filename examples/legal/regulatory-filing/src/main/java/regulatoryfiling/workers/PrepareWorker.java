package regulatoryfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareWorker implements Worker {
    @Override public String getTaskDefName() { return "rgf_prepare"; }

    @Override public TaskResult execute(Task task) {
        String filingType = (String) task.getInputData().get("filingType");
        String entity = (String) task.getInputData().get("entityName");
        System.out.println("  [prepare] Preparing " + filingType + " filing for " + entity);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("filingId", "FIL-" + System.currentTimeMillis());
        result.getOutputData().put("filingPackage", java.util.Map.of("type", filingType, "entity", entity, "documents", 5));
        return result;
    }
}
