package cloudwatchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates a CloudWatch alarm.
 * Input: namespace, metricName, threshold
 * Output: alarmName, alarmArn
 */
public class CreateAlarmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cw_create_alarm";
    }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        Object threshold = task.getInputData().get("threshold");
        String alarmName = metricName + "-high-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [alarm] Created alarm \"" + alarmName + "\" (threshold: " + threshold + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alarmName", "" + alarmName);
        result.getOutputData().put("alarmArn", "arn:aws:cloudwatch:us-east-1:123456789:alarm:" + alarmName);
        return result;
    }
}
