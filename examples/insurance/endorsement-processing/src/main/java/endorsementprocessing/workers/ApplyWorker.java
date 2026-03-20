package endorsementprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edp_apply";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [apply] Endorsement applied to %s%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applied", true);
        result.getOutputData().put("effectiveDate", "2024-03-15");
        return result;
    }
}
