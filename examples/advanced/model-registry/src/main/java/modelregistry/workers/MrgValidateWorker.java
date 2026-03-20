package modelregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MrgValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrg_validate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [validate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validationResult", "pass");
        result.getOutputData().put("checks", java.util.Map.of("schemaCheck", "pass", "performanceRegression", "pass", "biasAudit", "pass"));
        return result;
    }
}