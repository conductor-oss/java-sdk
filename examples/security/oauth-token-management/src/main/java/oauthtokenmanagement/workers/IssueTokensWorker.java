package oauthtokenmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Issues access and refresh tokens.
 * Input: issue_tokensData
 * Output: issue_tokens, processed
 */
public class IssueTokensWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "otm_issue_tokens";
    }

    @Override
    public TaskResult execute(Task task) {
        String scope = (String) task.getInputData().get("scope");
        if (scope == null) scope = "default";

        System.out.println("  [token] Access token issued with scope: " + scope);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("issue_tokens", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
