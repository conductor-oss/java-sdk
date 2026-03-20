package insurancerenewal.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProcessRenewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "irn_process_renew";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [renew] Policy %s renewed%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("renewed", true);
        result.getOutputData().put("effectiveDate", "2024-04-01");
        return result;
    }
}
