package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Distributes published content to channels.
 * Input: contentId, publishUrl, title
 * Output: channels, scheduledPosts, distributedAt
 */
public class DistributeContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_distribute_content";
    }

    @Override
    public TaskResult execute(Task task) {
        String title = (String) task.getInputData().get("title");
        if (title == null) title = "Untitled";

        System.out.println("  [distribute] Distributing \"" + title + "\" to channels");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("channels", List.of("twitter", "linkedin", "newsletter", "rss"));
        result.getOutputData().put("scheduledPosts", 4);
        result.getOutputData().put("distributedAt", "2026-03-08T12:15:00Z");
        return result;
    }
}
