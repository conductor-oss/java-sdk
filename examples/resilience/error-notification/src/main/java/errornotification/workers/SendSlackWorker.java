package errornotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for en_send_slack -- sends a Slack notification.
 *
 * Returns sent=true and the channel from input (defaults to "#alerts").
 */
public class SendSlackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_slack";
    }

    @Override
    public TaskResult execute(Task task) {
        String channel = task.getInputData().get("channel") != null
                ? String.valueOf(task.getInputData().get("channel"))
                : "#alerts";

        System.out.println("  [en_send_slack] Sending Slack notification to " + channel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("channel", channel);

        return result;
    }
}
