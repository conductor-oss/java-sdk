package videoprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "vid_publish"; }

    @Override
    public TaskResult execute(Task task) {
        String videoId = (String) task.getInputData().getOrDefault("videoId", "unknown");
        String title = (String) task.getInputData().getOrDefault("title", "Untitled");
        String hlsUrl = (String) task.getInputData().getOrDefault("hlsUrl", "");
        System.out.println("  [publish] Publishing video " + videoId + " (\"" + title + "\")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("publishUrl", "https://watch.example.com/v/" + videoId);
        result.getOutputData().put("publishedAt", "2025-01-15T10:30:00Z");
        result.getOutputData().put("status", "published");

        System.out.println("  [publish] Video " + videoId + " is now live.");
        return result;
    }
}
