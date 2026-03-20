package coldchain.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CheckThresholdsWorker implements Worker {
    @Override public String getTaskDefName() { return "cch_check_thresholds"; }
    @Override public TaskResult execute(Task task) {
        double temp = task.getInputData().get("currentTemp") instanceof Number ? ((Number)task.getInputData().get("currentTemp")).doubleValue() : 0;
        double min = task.getInputData().get("minTemp") instanceof Number ? ((Number)task.getInputData().get("minTemp")).doubleValue() : 0;
        double max = task.getInputData().get("maxTemp") instanceof Number ? ((Number)task.getInputData().get("maxTemp")).doubleValue() : 0;
        String status = (temp >= min && temp <= max) ? "ok" : "alert";
        System.out.println("  [check] " + temp + " C within [" + min + " C, " + max + " C]? -> " + status);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", status); return r;
    }
}
