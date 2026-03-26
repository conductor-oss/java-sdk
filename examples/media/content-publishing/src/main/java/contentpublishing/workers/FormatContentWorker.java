package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Formats content for distribution.
 * Input: contentId, contentType, approved
 * Output: formattedUrl, seoMetadata, formats
 */
public class FormatContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_format_content";
    }

    @Override
    public TaskResult execute(Task task) {
        String contentType = (String) task.getInputData().get("contentType");
        if (contentType == null) contentType = "unknown";

        System.out.println("  [format] Formatting " + contentType + " content");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedUrl", "https://cms.example.com/content/511-formatted");
        result.getOutputData().put("seoMetadata", Map.of(
                "metaTitle", "Optimizing Cloud Costs",
                "metaDescription", "A guide to reducing cloud spend"));
        result.getOutputData().put("formats", List.of("html", "amp", "rss"));
        return result;
    }
}
