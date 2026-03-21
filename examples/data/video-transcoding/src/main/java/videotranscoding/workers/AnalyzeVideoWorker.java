package videotranscoding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analyzes the source video to extract codec, resolution, duration, and bitrate metadata.
 */
public class AnalyzeVideoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vt_analyze_video";
    }

    @Override
    public TaskResult execute(Task task) {
        String videoUrl = (String) task.getInputData().get("videoUrl");
        System.out.println("[vt_analyze_video] Analyzing source video: " + videoUrl);
        System.out.println("  Detected codec: h264, resolution: 3840x2160, duration: 124s");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("codec", "h264");
        output.put("resolution", "3840x2160");
        output.put("duration", 124);
        output.put("bitrate", 45000);
        output.put("frameRate", 30);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
