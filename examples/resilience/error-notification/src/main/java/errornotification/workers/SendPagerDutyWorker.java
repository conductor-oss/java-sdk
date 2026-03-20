package errornotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for en_send_pagerduty -- sends a PagerDuty alert.
 *
 * Returns sent=true.
 */
public class SendPagerDutyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_pagerduty";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [en_send_pagerduty] Sending PagerDuty alert");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);

        return result;
    }
}
