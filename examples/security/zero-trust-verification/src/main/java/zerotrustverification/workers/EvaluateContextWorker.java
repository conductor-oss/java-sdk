package zerotrustverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateContextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zt_evaluate_context";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [context] Corporate network, business hours, normal behavior");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("evaluate_context", true);
        result.addOutputData("processed", true);
        return result;
    }
}
