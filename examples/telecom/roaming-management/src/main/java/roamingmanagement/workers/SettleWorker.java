package roamingmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SettleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rmg_settle";
    }

    @Override
    public TaskResult execute(Task task) {

        String homeNetwork = (String) task.getInputData().get("homeNetwork");
        String visitedNetwork = (String) task.getInputData().get("visitedNetwork");
        System.out.println("  [settle] Inter-carrier settlement processed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("settled", true);
        result.getOutputData().put("settlementId", "STL-roaming-management-001");
        return result;
    }
}
