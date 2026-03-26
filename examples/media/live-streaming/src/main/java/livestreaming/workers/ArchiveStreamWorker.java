package livestreaming.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Archives the completed stream for VOD access.
 * Input: streamId, title, duration
 * Output: archiveUrl, archiveSize, archivedAt, thumbnailUrl
 */
public class ArchiveStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lsm_archive_stream";
    }

    @Override
    public TaskResult execute(Task task) {
        String title = (String) task.getInputData().get("title");
        if (title == null) title = "Untitled";

        Object durationObj = task.getInputData().get("duration");
        int durationMin = 0;
        if (durationObj != null) {
            durationMin = (int) Math.round(Double.parseDouble(durationObj.toString()) / 60.0);
        }

        System.out.println("  [archive] Archiving " + durationMin + " min stream \"" + title + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("archiveUrl", "https://vod.example.com/archive/522");
        result.getOutputData().put("archiveSize", "4.2 GB");
        result.getOutputData().put("archivedAt", "2026-03-08T20:00:00Z");
        result.getOutputData().put("thumbnailUrl", "https://cdn.example.com/thumbs/522.jpg");
        return result;
    }
}
