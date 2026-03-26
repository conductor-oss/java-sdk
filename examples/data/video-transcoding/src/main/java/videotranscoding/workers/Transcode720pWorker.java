package videotranscoding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transcodes the source video to 720p resolution.
 */
public class Transcode720pWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vt_transcode_720p";
    }

    @Override
    public TaskResult execute(Task task) {
        String videoUrl = (String) task.getInputData().get("videoUrl");
        String codec = (String) task.getInputData().get("codec");
        Object duration = task.getInputData().get("duration");
        System.out.println("[vt_transcode_720p] Transcoding to 720p (codec=" + codec + ", duration=" + duration + "s)");
        System.out.println("  Output: /output/video_720p.mp4 (85 MB)");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "/output/video_720p.mp4");
        output.put("resolution", "1280x720");
        output.put("fileSizeMb", 85);
        output.put("bitrateKbps", 5500);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
