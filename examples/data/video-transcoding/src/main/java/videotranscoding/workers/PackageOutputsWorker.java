package videotranscoding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packages all transcoded outputs into a delivery manifest with format count and total size.
 */
public class PackageOutputsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vt_package_outputs";
    }

    @Override
    public TaskResult execute(Task task) {
        String result720 = (String) task.getInputData().get("result720");
        String result1080 = (String) task.getInputData().get("result1080");
        String result4k = (String) task.getInputData().get("result4k");
        Object originalDuration = task.getInputData().get("originalDuration");

        System.out.println("[vt_package_outputs] Packaging transcoded outputs");
        System.out.println("  720p: " + result720);
        System.out.println("  1080p: " + result1080);
        System.out.println("  4K: " + result4k);
        System.out.println("  Original duration: " + originalDuration + "s");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("formatCount", 3);
        output.put("totalSize", "975 MB");
        output.put("manifest", Map.of(
                "formats", List.of(
                        Map.of("resolution", "720p", "path", result720 != null ? result720 : "/output/video_720p.mp4"),
                        Map.of("resolution", "1080p", "path", result1080 != null ? result1080 : "/output/video_1080p.mp4"),
                        Map.of("resolution", "4K", "path", result4k != null ? result4k : "/output/video_4k.mp4")
                ),
                "duration", originalDuration != null ? originalDuration : 0
        ));
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
