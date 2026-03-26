package taskdomains.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes data and tags output with the worker group name.
 * When used with task domains, this worker can be routed to
 * specific worker groups (e.g., "gpu") so only workers in
 * that domain pick up the task.
 */
public class TdProcessWorker implements Worker {

    private final String workerGroup;

    public TdProcessWorker() {
        this("default-group");
    }

    public TdProcessWorker(String workerGroup) {
        this.workerGroup = workerGroup;
    }

    @Override
    public String getTaskDefName() {
        return "td_process";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("data");

        String type = "unknown";
        Object value = null;
        if (data != null) {
            type = data.get("type") != null ? data.get("type").toString() : "unknown";
            value = data.get("value");
        }

        System.out.println("  [td_process] Processing type=" + type
                + ", value=" + value + " (worker: " + workerGroup + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed");
        result.getOutputData().put("worker", workerGroup);
        return result;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }
}
