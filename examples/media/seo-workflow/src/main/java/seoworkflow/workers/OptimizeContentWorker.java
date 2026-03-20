package seoworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OptimizeContentWorker implements Worker {
    @Override public String getTaskDefName() { return "seo_optimize_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [optimize] Processing " + task.getInputData().getOrDefault("optimizedPages", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("optimizedPages", 12);
        r.getOutputData().put("metaDescriptionsAdded", 8);
        r.getOutputData().put("headingsOptimized", 15);
        r.getOutputData().put("internalLinksAdded", 22);
        return r;
    }
}
