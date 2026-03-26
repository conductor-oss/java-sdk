package imagepipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class WatermarkImageWorker implements Worker {
    @Override public String getTaskDefName() { return "imp_watermark_image"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [watermark] Processing " + task.getInputData().getOrDefault("watermarkedPaths", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("watermarkedPaths", List.of("s3://media/images/513/watermarked_200x150.jpg", "s3://media/images/513/watermarked_800x600.jpg"));
        r.getOutputData().put("watermarkApplied", true);
        return r;
    }
}
