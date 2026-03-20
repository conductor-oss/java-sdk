package imagepipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class OptimizeImageWorker implements Worker {
    @Override public String getTaskDefName() { return "imp_optimize_image"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [optimize] Processing " + task.getInputData().getOrDefault("optimizedPaths", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("optimizedPaths", List.of("s3://media/images/513/optimized_200x150.jpg", "s3://media/images/513/optimized_800x600.jpg"));
        r.getOutputData().put("savedPercent", 42);
        r.getOutputData().put("averageQuality", 85);
        return r;
    }
}
