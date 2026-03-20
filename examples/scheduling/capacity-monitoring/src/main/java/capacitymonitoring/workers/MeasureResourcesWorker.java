package capacitymonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MeasureResourcesWorker implements Worker {
    @Override public String getTaskDefName() { return "cap_measure_resources"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [measure] Measuring resources for cluster " + task.getInputData().get("cluster"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("cpuUsage", 72); r.getOutputData().put("memoryUsage", 68);
        r.getOutputData().put("diskUsage", 81); r.getOutputData().put("nodeCount", 12);
        return r;
    }
}
