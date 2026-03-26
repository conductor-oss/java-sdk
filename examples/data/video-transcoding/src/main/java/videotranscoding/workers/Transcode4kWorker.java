package videotranscoding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transcodes the source video to 4K resolution.
 */
public class Transcode4kWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vt_transcode_4k";
    }

    @Override
    public TaskResult execute(Task task) {
        String videoUrl = (String) task.getInputData().get("videoUrl");
        String codec = (String) task.getInputData().get("codec");
        Object duration = task.getInputData().get("duration");
        System.out.println("[vt_transcode_4k] Transcoding to 4K (codec=" + codec + ", duration=" + duration + "s)");
        System.out.println("  Output: /output/video_4k.mp4 (680 MB)");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", "/output/video_4k.mp4");
        output.put("resolution", "3840x2160");
        output.put("fileSizeMb", 680);
        output.put("bitrateKbps", 44000);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
