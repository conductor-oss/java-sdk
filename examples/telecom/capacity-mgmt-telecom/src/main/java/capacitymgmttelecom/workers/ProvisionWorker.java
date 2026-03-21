package capacitymgmttelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProvisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmt_provision";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [provision] 2 cell towers provisioned and configured");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("provisionId", "PROV-capacity-mgmt-telecom-001");
        result.getOutputData().put("newCapacity", "150%");
        result.getOutputData().put("towersAdded", 2);
        return result;
    }
}
