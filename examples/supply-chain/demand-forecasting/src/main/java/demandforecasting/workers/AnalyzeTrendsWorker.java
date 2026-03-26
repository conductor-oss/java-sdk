package demandforecasting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class AnalyzeTrendsWorker implements Worker {
    @Override public String getTaskDefName() { return "df_analyze_trends"; }
    @Override public TaskResult execute(Task task) {
        double avgGrowth = 8.5;
        System.out.println("  [trends] Upward trend: avg growth " + avgGrowth + "% month-over-month");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("trends", Map.of("avgGrowth", avgGrowth, "seasonality", "Q4 peak"));
        r.getOutputData().put("direction", "upward");
        return r;
    }
}
