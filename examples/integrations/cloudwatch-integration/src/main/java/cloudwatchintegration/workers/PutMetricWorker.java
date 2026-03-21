package cloudwatchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publishes a metric to CloudWatch.
 * Input: namespace, metricName, value
 * Output: published, timestamp
 */
public class PutMetricWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cw_put_metric";
    }

    @Override
    public TaskResult execute(Task task) {
        String namespace = (String) task.getInputData().get("namespace");
        String metricName = (String) task.getInputData().get("metricName");
        Object value = task.getInputData().get("value");
        System.out.println("  [metric] Published " + namespace + "/" + metricName + " = " + value);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("timestamp", "" + java.time.Instant.now().toString());
        return result;
    }
}
