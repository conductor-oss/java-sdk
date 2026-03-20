package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Reviews draft content and assigns a score.
 * Input: contentId, draftVersion, wordCount
 * Output: reviewScore, reviewer, reviewNotes
 */
public class ReviewContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_review_content";
    }

    @Override
    public TaskResult execute(Task task) {
        Object draftVersion = task.getInputData().get("draftVersion");
        Object wordCount = task.getInputData().get("wordCount");
        if (draftVersion == null) draftVersion = 1;
        if (wordCount == null) wordCount = 0;

        System.out.println("  [review] Reviewing draft v" + draftVersion + " (" + wordCount + " words)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reviewScore", 8.5);
        result.getOutputData().put("reviewer", "editor-01");
        result.getOutputData().put("reviewNotes", "Strong intro, needs minor copy edits");
        return result;
    }
}
