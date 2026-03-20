package firmwareupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DownloadWorker implements Worker {
    @Override public String getTaskDefName() { return "fw_download"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [download] Processing " + task.getInputData().getOrDefault("firmwarePath", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("firmwarePath", "/tmp/firmware/v2.5.0/firmware.bin");
        r.getOutputData().put("downloadedSizeMb", 12.5);
        r.getOutputData().put("downloadTimeMs", 3200);
        return r;
    }
}
