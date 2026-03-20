package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publishes content to production.
 * Input: contentId, formattedUrl, seoMetadata
 * Output: publishUrl, publishedAt, cacheInvalidated
 */
public class PublishContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_publish_content";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [publish] Publishing to production");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("publishUrl", "https://blog.example.com/optimizing-cloud-costs");
        result.getOutputData().put("publishedAt", "2026-03-08T12:00:00Z");
        result.getOutputData().put("cacheInvalidated", true);
        return result;
    }
}
