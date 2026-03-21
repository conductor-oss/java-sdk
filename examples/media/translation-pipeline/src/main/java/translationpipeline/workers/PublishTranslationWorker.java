package translationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishTranslationWorker implements Worker {
    @Override public String getTaskDefName() { return "trn_publish_translation"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Processing " + task.getInputData().getOrDefault("translationUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("translationUrl", "https://content.example.com/fr/517-translated");
        r.getOutputData().put("publishedAt", "2026-03-08T12:00:00Z");
        r.getOutputData().put("locale", "fr-FR");
        return r;
    }
}
