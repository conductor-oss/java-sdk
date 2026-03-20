package contentreviewpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for crp_ai_draft task -- generates an AI content draft.
 *
 * Inputs:
 *   - topic:    the content topic
 *   - audience: the target audience
 *
 * Outputs:
 *   - content:   generated draft content string incorporating topic and audience
 *   - wordCount: 42 (deterministic.word count)
 *   - model:     "claude-3.5" (the AI model used)
 */
public class CrpAiDraftWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crp_ai_draft";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        String audience = (String) task.getInputData().get("audience");

        String content = "Draft content about " + topic + " for " + audience;

        System.out.println("  [crp_ai_draft] Generated draft: " + content);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("wordCount", 42);
        result.getOutputData().put("model", "claude-3.5");

        return result;
    }
}
