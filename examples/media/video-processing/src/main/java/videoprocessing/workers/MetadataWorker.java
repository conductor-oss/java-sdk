package videoprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class MetadataWorker implements Worker {
    @Override public String getTaskDefName() { return "vid_metadata"; }

    @Override
    public TaskResult execute(Task task) {
        String videoId = (String) task.getInputData().getOrDefault("videoId", "unknown");
        String title = (String) task.getInputData().getOrDefault("title", "Untitled");
        Object durationObj = task.getInputData().get("duration");
        int duration = durationObj instanceof Number ? ((Number) durationObj).intValue() : 0;
        System.out.println("  [metadata] Extracting metadata for video " + videoId + " (\"" + title + "\")...");

        Map<String, Object> metadata = Map.of(
                "videoId", videoId,
                "title", title,
                "duration", duration,
                "resolutions", task.getInputData().getOrDefault("resolutions", "[]"),
                "contentType", "video/mp4"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metadata", metadata);

        System.out.println("  [metadata] Metadata indexed for video " + videoId + ".");
        return result;
    }
}
