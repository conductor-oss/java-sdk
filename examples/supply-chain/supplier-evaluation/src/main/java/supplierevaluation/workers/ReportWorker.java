package supplierevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "spe_report"; }
    @Override public TaskResult execute(Task task) {
        List<?> rankings = (List<?>) task.getInputData().get("rankings");
        int count = rankings != null ? rankings.size() : 0;
        System.out.println("  [report] Generated evaluation report for " + count + " suppliers");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("report", Map.of("suppliersRanked", count, "generated", true));
        return r;
    }
}
