package reinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReconcileWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rin_reconcile";
    }

    @Override
    public TaskResult execute(Task task) {

        String cessionId = (String) task.getInputData().get("cessionId");
        System.out.printf("  [reconcile] Cession %s reconciled%n", cessionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reconciled", true);
        result.getOutputData().put("variance", 0);
        return result;
    }
}
