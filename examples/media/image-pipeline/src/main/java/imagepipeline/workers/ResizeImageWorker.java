package imagepipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ResizeImageWorker implements Worker {
    @Override public String getTaskDefName() { return "imp_resize_image"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [resize] Processing " + task.getInputData().getOrDefault("resizedPaths", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("resizedPaths", java.util.List.of("s3://media/images/513/resized_200x150.jpg", "s3://media/images/513/resized_800x600.jpg"));
        return r;
    }
}
