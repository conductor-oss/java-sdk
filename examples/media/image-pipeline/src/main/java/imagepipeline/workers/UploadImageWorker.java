package imagepipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UploadImageWorker implements Worker {
    @Override public String getTaskDefName() { return "imp_upload_image"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [upload] Processing " + task.getInputData().getOrDefault("storagePath", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("storagePath", "s3://media/images/513/original.jpg");
        r.getOutputData().put("originalWidth", 4000);
        r.getOutputData().put("originalHeight", 3000);
        r.getOutputData().put("fileSizeKb", 3200);
        r.getOutputData().put("format", "jpeg");
        return r;
    }
}
