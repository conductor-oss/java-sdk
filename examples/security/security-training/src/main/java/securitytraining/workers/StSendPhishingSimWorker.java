package securitytraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends phishing process to department employees.
 * Input: send_phishing_simData (from assign step)
 * Output: send_phishing_sim, processed
 */
public class StSendPhishingSimWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_send_phishing_sim";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [phishing] Phishing process sent: 8% click rate");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("send_phishing_sim", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
