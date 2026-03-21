package securityorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DecideActionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soar_decide_action";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [decide] Action: isolate host, block C2 domain, collect forensics");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("decide_action", true);
        result.addOutputData("processed", true);
        return result;
    }
}
