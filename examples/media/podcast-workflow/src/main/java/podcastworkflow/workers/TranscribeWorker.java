package podcastworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TranscribeWorker implements Worker {
    @Override public String getTaskDefName() { return "pod_transcribe"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [transcribe] Processing " + task.getInputData().getOrDefault("transcriptUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("transcriptUrl", "s3://media/podcasts/514/transcript.srt");
        r.getOutputData().put("wordCount", 6200);
        r.getOutputData().put("language", "en-US");
        r.getOutputData().put("confidence", 0.96);
        return r;
    }
}
