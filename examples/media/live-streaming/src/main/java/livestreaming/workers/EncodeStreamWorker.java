package livestreaming.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Encodes the live stream with adaptive bitrate profiles.
 * Input: streamId, ingestUrl, resolution
 * Output: encodedUrl, adaptiveBitrates, codec, latencyMs
 */
public class EncodeStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lsm_encode_stream";
    }

    @Override
    public TaskResult execute(Task task) {
        String resolution = (String) task.getInputData().get("resolution");
        if (resolution == null) resolution = "720p";

        System.out.println("  [encode] Encoding at " + resolution + " with adaptive bitrate");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("encodedUrl", "https://encode.example.com/live/522/master.m3u8");
        result.getOutputData().put("adaptiveBitrates", List.of("1080p@5Mbps", "550p@3Mbps", "480p@1.5Mbps", "360p@800Kbps"));
        result.getOutputData().put("codec", "h264");
        result.getOutputData().put("latencyMs", 5000);
        return result;
    }
}
