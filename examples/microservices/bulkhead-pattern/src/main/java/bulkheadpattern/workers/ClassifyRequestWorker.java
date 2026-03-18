package bulkheadpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies an incoming request into a resource pool based on priority.
 * Input: serviceName, priority
 * Output: pool, maxConcurrency
 *
 * Logic:
 *   priority "high" -> pool "premium-pool", maxConcurrency 50
 *   otherwise       -> pool "standard-pool", maxConcurrency 10
 */
public class ClassifyRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bh_classify_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        String priority = (String) task.getInputData().get("priority");
        if (serviceName == null) serviceName = "unknown";
        if (priority == null) priority = "normal";

        System.out.println("  [bh_classify_request] " + serviceName + " priority=" + priority);

        String pool;
        int maxConcurrency;

        if ("high".equals(priority)) {
            pool = "premium-pool";
            maxConcurrency = 50;
        } else {
            pool = "standard-pool";
            maxConcurrency = 10;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("pool", pool);
        result.getOutputData().put("maxConcurrency", maxConcurrency);
        return result;
    }
}
