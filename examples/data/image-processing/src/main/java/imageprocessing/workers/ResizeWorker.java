package imageprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resizes an image to multiple variants.
 * Input: imageData, sizes (list of {w, h})
 * Output: variants (list), variantCount
 */
public class ResizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_resize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> sizes = (List<Map<String, Object>>) task.getInputData().get("sizes");
        if (sizes == null || sizes.isEmpty()) {
            sizes = List.of(
                    Map.of("w", 1920, "h", 1080),
                    Map.of("w", 800, "h", 450),
                    Map.of("w", 150, "h", 150)
            );
        }

        List<Map<String, Object>> variants = new ArrayList<>();
        for (Map<String, Object> s : sizes) {
            int w = ((Number) s.get("w")).intValue();
            int h = ((Number) s.get("h")).intValue();
            Map<String, Object> variant = new LinkedHashMap<>();
            variant.put("width", w);
            variant.put("height", h);
            variant.put("size", (Math.round((long) w * h * 3 / 1024)) + "KB");
            variants.add(variant);
        }

        System.out.println("  [resize] Created " + variants.size() + " variants");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("variants", variants);
        result.getOutputData().put("variantCount", variants.size());
        return result;
    }
}
