package healthdashboard.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RenderDashboardWorker implements Worker {
    @Override public String getTaskDefName() { return "hd_render_dashboard"; }
    @Override public TaskResult execute(Task task) {
        boolean allHealthy = "healthy".equals(task.getInputData().get("apiStatus")) && "healthy".equals(task.getInputData().get("dbStatus")) && "healthy".equals(task.getInputData().get("cacheStatus"));
        System.out.println("  [render] Rendering dashboard for " + task.getInputData().get("environment") + " - overall: " + (allHealthy ? "GREEN" : "DEGRADED"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("overallHealth", allHealthy ? "GREEN" : "DEGRADED");
        r.getOutputData().put("dashboardUrl", "https://dashboard.example.com/health");
        r.getOutputData().put("components", 3);
        return r;
    }
}
