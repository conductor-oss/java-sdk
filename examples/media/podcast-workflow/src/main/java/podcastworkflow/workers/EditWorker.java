package podcastworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EditWorker implements Worker {
    @Override public String getTaskDefName() { return "pod_edit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [edit] Processing " + task.getInputData().getOrDefault("editedAudioUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("editedAudioUrl", "s3://media/podcasts/514/edited.mp3");
        r.getOutputData().put("finalDuration", 42);
        r.getOutputData().put("fileSizeMb", 38);
        r.getOutputData().put("bitrate", "128kbps");
        r.getOutputData().put("silenceRemoved", "3.5 min");
        return r;
    }
}
