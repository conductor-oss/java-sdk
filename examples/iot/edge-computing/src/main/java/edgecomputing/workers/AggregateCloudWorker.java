package edgecomputing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AggregateCloudWorker implements Worker {
    @Override public String getTaskDefName() { return "edg_aggregate_cloud"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Processing " + task.getInputData().getOrDefault("aggregated", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("aggregated", true);
        r.getOutputData().put("storedInTimeseries", true);
        r.getOutputData().put("dashboardUpdated", true);
        r.getOutputData().put("totalEdgeJobs24h", 15200);
        return r;
    }
}
