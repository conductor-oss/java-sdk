package contentarchival.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CompressWorker implements Worker {
    @Override public String getTaskDefName() { return "car_compress"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [compress] Processing " + task.getInputData().getOrDefault("compressedPath", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("compressedPath", "s3://staging/archive/528/compressed.tar.zst");
        r.getOutputData().put("compressedSizeMb", "compressedMb");
        r.getOutputData().put("compressionRatio", 0.35);
        r.getOutputData().put("checksum", "sha256:fedcba983214");
        return r;
    }
}
