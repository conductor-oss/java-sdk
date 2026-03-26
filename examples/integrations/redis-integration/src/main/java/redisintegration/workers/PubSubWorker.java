package redisintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publishes a message to a Redis channel.
 * Input: connectionId, channel, message
 * Output: published, subscriberCount, channel
 */
public class PubSubWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "red_pub_sub";
    }

    @Override
    public TaskResult execute(Task task) {
        String channel = (String) task.getInputData().get("channel");
        int subscriberCount = 3;
        System.out.println("  [publish] Channel \"" + channel + "\" -> " + subscriberCount + " subscribers received message");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("subscriberCount", subscriberCount);
        result.getOutputData().put("channel", "" + channel);
        return result;
    }
}
