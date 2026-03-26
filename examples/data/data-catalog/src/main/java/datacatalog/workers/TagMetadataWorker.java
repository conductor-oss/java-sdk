package datacatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Tags data assets with metadata labels.
 * Input: classifiedAssets
 * Output: tagged, tagCount
 */
public class TagMetadataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cg_tag_metadata";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) task.getInputData().get("classifiedAssets");
        if (assets == null) {
            assets = List.of();
        }

        int tagCount = 0;
        List<Map<String, Object>> tagged = new ArrayList<>();

        for (Map<String, Object> asset : assets) {
            Map<String, Object> t = new LinkedHashMap<>(asset);
            List<String> tags = new ArrayList<>();
            tags.add((String) asset.getOrDefault("category", "unknown"));
            tags.add((String) asset.getOrDefault("type", "unknown"));
            tags.add((String) asset.getOrDefault("sensitivity", "unknown"));

            Object hasPII = asset.get("hasPII");
            if (Boolean.TRUE.equals(hasPII)) {
                tags.add("pii");
                tags.add("gdpr-relevant");
            }

            Object rowCountObj = asset.get("rowCount");
            if (rowCountObj instanceof Number && ((Number) rowCountObj).intValue() > 100000) {
                tags.add("large-dataset");
            }

            tagCount += tags.size();
            t.put("tags", tags);
            tagged.add(t);
        }

        System.out.println("  [tag] Applied " + tagCount + " tags across " + tagged.size() + " assets");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tagged", tagged);
        result.getOutputData().put("tagCount", tagCount);
        return result;
    }
}
