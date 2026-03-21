package endorsementprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edp_approve";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [approve] Endorsement approved");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        return result;
    }
}
