package capacitymonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ForecastWorker implements Worker {
    @Override public String getTaskDefName() { return "cap_forecast"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [forecast] Forecasting capacity for next " + task.getInputData().get("forecastDays") + " days");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("daysUntilCpuFull", 45); r.getOutputData().put("daysUntilMemFull", 60);
        r.getOutputData().put("daysUntilDiskFull", 22); r.getOutputData().put("recommendation", "Add 2 nodes within 3 weeks");
        return r;
    }
}
