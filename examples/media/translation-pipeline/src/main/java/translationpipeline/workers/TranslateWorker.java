package translationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TranslateWorker implements Worker {
    @Override public String getTaskDefName() { return "trn_translate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [translate] Processing " + task.getInputData().getOrDefault("translatedText", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("translatedText", "Automatisez vos flux de travail avec Conductor pour une orchestration efficace.");
        r.getOutputData().put("qualityScore", 0.92);
        r.getOutputData().put("wordCount", 11);
        r.getOutputData().put("modelVersion", "nmt-v4.2");
        return r;
    }
}
