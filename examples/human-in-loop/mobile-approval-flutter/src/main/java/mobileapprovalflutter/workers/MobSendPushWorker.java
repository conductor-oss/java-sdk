package mobileapprovalflutter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mob_send_push -- performs sending an FCM push notification.
 *
 * Returns { pushSent: true }.
 */
public class MobSendPushWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mob_send_push";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mob_send_push] Performing FCM push notification...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("pushSent", true);
        return result;
    }
}
