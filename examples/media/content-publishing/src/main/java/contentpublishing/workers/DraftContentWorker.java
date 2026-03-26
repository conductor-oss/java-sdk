package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Creates a draft of the content.
 * Input: contentId, authorId, contentType, title
 * Output: draftVersion, wordCount, readTime, slug
 */
public class DraftContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_draft_content";
    }

    @Override
    public TaskResult execute(Task task) {
        String title = (String) task.getInputData().get("title");
        String authorId = (String) task.getInputData().get("authorId");
        if (title == null) title = "Untitled";
        if (authorId == null) authorId = "unknown";

        System.out.println("  [draft] Creating draft: \"" + title + "\" by " + authorId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("draftVersion", 1);
        result.getOutputData().put("wordCount", 1450);
        result.getOutputData().put("readTime", "6 min");
        result.getOutputData().put("slug", "optimizing-cloud-costs");
        return result;
    }
}
