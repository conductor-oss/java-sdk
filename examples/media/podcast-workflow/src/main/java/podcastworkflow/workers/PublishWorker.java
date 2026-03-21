package podcastworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "pod_publish"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Processing " + task.getInputData().getOrDefault("episodeUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("episodeUrl", "https://podcast.example.com/shows/tech-talks/ep-514");
        r.getOutputData().put("rssUrl", "https://podcast.example.com/feeds/tech-talks.xml");
        r.getOutputData().put("publishedAt", "2026-03-08T06:00:00Z");
        return r;
    }
}
