package firmwareupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckVersionWorker implements Worker {
    @Override public String getTaskDefName() { return "fw_check_version"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [check] Processing " + task.getInputData().getOrDefault("updateAvailable", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("updateAvailable", true);
        r.getOutputData().put("downloadUrl", "https://firmware.example.com/v2.5.0/firmware.bin");
        r.getOutputData().put("checksum", "sha256:abc123def456ghi789");
        r.getOutputData().put("releaseNotes", "Bug fixes and security patches");
        r.getOutputData().put("sizeMb", 12.5);
        return r;
    }
}
