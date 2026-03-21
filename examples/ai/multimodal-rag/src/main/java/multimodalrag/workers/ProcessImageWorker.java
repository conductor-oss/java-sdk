package multimodalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that processes image references by extracting visual features.
 * Returns a list of features for each image, including detected objects,
 * layout description, and generated caption.
 */
public class ProcessImageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mm_process_image";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> imageRefs =
                (List<Map<String, String>>) task.getInputData().get("imageRefs");
        if (imageRefs == null) {
            imageRefs = List.of();
        }

        List<Map<String, Object>> features = List.of(
                Map.of(
                        "imageId", "img-001",
                        "objects", List.of("diagram", "workflow", "arrows", "nodes"),
                        "layout", "A flowchart showing a multi-step orchestration pipeline",
                        "caption", "Workflow orchestration diagram with branching logic and parallel tasks"
                ),
                Map.of(
                        "imageId", "img-002",
                        "objects", List.of("chart", "bars", "labels", "legend"),
                        "layout", "A bar chart comparing performance metrics across models",
                        "caption", "Performance comparison chart showing latency and throughput for different models"
                )
        );

        System.out.println("  [process_image] Extracted features from " + imageRefs.size()
                + " images -> " + features.size() + " feature sets");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("imageFeatures", features);
        return result;
    }
}
