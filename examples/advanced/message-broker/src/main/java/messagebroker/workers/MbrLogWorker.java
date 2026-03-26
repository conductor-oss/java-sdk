package messagebroker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MbrLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mbr_log";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [log] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String logId = "LOG-" + Long.toString(System.currentTimeMillis(), 36);
        result.getOutputData().put("logId", logId);
        result.getOutputData().put("logged", true);
        return result;
    }
}