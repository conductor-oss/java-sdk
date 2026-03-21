package videotranscoding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transcodes the source video to 1080p resolution.
 */
public class Transcode1080pWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vt_transcode_1080p";
    }

    @Override
    public TaskResult execute(Task task) {
        String videoUrl = (String) task.getInputData().get("videoUrl");
        String codec = (String) task.getInputData().get("codec");
        Object duration = task.getInputData().get("duration");
        System.out.println("[vt_transcode_1080p] Transcoding to 1080p (codec=" + codec + ", duration=" + duration + "s)");
        System.out.println("  Output: /output/video_1080p.mp4 (210 MB)");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "/output/video_1080p.mp4");
        output.put("resolution", "1920x1080");
        output.put("fileSizeMb", 210);
        output.put("bitrateKbps", 15000);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
