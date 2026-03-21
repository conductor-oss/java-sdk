package workerscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WksMonitorQueueWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wks_monitor_queue";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [monitor] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queueDepth", 450);
        result.getOutputData().put("avgProcessingMs", 200);
        result.getOutputData().put("inFlightTasks", 30);
        return result;
    }
}