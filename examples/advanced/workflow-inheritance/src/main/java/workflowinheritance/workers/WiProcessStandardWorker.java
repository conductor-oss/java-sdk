package workflowinheritance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WiProcessStandardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wi_process_standard";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process-standard] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "standard_processed");
        result.getOutputData().put("sla", "24h");
        return result;
    }
}