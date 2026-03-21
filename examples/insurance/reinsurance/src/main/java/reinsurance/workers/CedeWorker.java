package reinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CedeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rin_cede";
    }

    @Override
    public TaskResult execute(Task task) {

        String treatyId = (String) task.getInputData().get("treatyId");
        System.out.printf("  [cede] Ceded under treaty %s%n", treatyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cessionId", "CES-reinsurance-001");
        result.getOutputData().put("ceded", true);
        return result;
    }
}
