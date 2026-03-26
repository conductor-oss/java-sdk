package workflowtemplating.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtmVerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtm_verify";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [verify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("match", true);
        return result;
    }
}