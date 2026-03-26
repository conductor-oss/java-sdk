package thresholdalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Sends a warning alert to Slack.
 */
public class SendWarning implements Worker {

    @Override
    public String getTaskDefName() {
        return "th_send_warning";
    }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        Object currentValue = task.getInputData().get("currentValue");

        System.out.println("[th_send_warning] WARNING: Sending alert for " + metricName + " = " + currentValue);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("warned", true);
        result.getOutputData().put("channel", "slack");
        result.getOutputData().put("sentTo", "#ops-alerts");
        return result;
    }
}
