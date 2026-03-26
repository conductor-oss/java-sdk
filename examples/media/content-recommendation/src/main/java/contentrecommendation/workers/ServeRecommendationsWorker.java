package contentrecommendation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServeRecommendationsWorker implements Worker {
    @Override public String getTaskDefName() { return "crm_serve_recommendations"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [serve] Processing " + task.getInputData().getOrDefault("recommendations", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recommendations", "items");
        r.getOutputData().put("servedCount", "items.length");
        r.getOutputData().put("responseTimeMs", 32);
        r.getOutputData().put("trackingId", "TRK-520-001");
        return r;
    }
}
