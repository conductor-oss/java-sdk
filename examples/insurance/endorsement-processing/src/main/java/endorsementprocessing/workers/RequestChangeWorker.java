package endorsementprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RequestChangeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edp_request_change";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [request] Endorsement requested on %s%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endorsementId", "END-endorsement-processing-001");
        return result;
    }
}
