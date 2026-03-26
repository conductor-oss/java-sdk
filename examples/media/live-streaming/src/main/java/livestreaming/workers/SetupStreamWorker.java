package livestreaming.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sets up a live stream with ingest URL and stream key.
 * Input: streamId, channelId, title, resolution
 * Output: ingestUrl, streamKey, startedAt
 */
public class SetupStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lsm_setup_stream";
    }

    @Override
    public TaskResult execute(Task task) {
        String title = (String) task.getInputData().get("title");
        String resolution = (String) task.getInputData().get("resolution");
        if (title == null) title = "Untitled";
        if (resolution == null) resolution = "720p";

        System.out.println("  [setup] Setting up stream \"" + title + "\" at " + resolution);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ingestUrl", "rtmp://ingest.example.com/live/522");
        result.getOutputData().put("streamKey", "sk-522-abc123");
        result.getOutputData().put("startedAt", "2026-03-08T18:00:00Z");
        return result;
    }
}
