package productcatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Enriches product metadata with SEO title, tags, and slug.
 */
public class EnrichProductWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prd_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        String category = (String) task.getInputData().get("category");
        String name = (String) task.getInputData().get("name");

        if (name == null) name = "unknown";
        if (category == null) category = "general";

        String seoTitle = "Buy " + name + " | Best " + category;
        List<String> tags = List.of(category, "new-arrival", "featured");
        String slug = name.toLowerCase().replaceAll("\\s+", "-");

        System.out.println("  [enrich] Product " + productId + ": slug=\"" + slug + "\", tags=" + String.join(",", tags));

        Map<String, Object> enrichedData = new LinkedHashMap<>();
        enrichedData.put("seoTitle", seoTitle);
        enrichedData.put("tags", tags);
        enrichedData.put("slug", slug);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("enrichedData", enrichedData);
        result.setOutputData(output);
        return result;
    }
}
