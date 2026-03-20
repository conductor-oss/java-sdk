package workflowinheritance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WiProcessPremiumWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wi_process_premium";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process-premium] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "premium_processed");
        result.getOutputData().put("sla", "2h");
        return result;
    }
}