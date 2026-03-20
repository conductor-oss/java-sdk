package demandforecasting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ForecastWorker implements Worker {
    @Override public String getTaskDefName() { return "df_forecast"; }
    @Override public TaskResult execute(Task task) {
        Map<String, Object> forecast = Map.of("Q3", 5400, "Q4", 5500, "total", 12600);
        System.out.println("  [forecast] " + task.getInputData().get("horizon") + " forecast: 12600 units");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("forecast", forecast);
        return r;
    }
}
