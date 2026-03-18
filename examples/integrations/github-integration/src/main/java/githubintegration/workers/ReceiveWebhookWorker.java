package githubintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives a GitHub webhook push event.
 * Input: repo, branch, commitMessage
 * Output: prTitle, eventType, receivedAt
 *
 * This worker is requires external setup — webhook reception requires
 * a real webhook endpoint that receives GitHub push events.
 */
public class ReceiveWebhookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gh_receive_webhook";
    }

    @Override
    public TaskResult execute(Task task) {
        String commitMessage = (String) task.getInputData().get("commitMessage");
        String repo = (String) task.getInputData().get("repo");
        String branch = (String) task.getInputData().get("branch");
        System.out.println("  [webhook] Push to " + repo + "/" + branch);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prTitle", "feat: " + commitMessage);
        result.getOutputData().put("eventType", "push");
        result.getOutputData().put("receivedAt", java.time.Instant.now().toString());
        return result;
    }
}
