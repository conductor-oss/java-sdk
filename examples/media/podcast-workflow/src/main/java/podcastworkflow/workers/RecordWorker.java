package podcastworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordWorker implements Worker {
    @Override public String getTaskDefName() { return "pod_record"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [record] Processing " + task.getInputData().getOrDefault("rawAudioUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rawAudioUrl", "s3://media/podcasts/514/raw.wav");
        r.getOutputData().put("durationMinutes", 48);
        r.getOutputData().put("sampleRate", 48000);
        r.getOutputData().put("channels", 2);
        return r;
    }
}
