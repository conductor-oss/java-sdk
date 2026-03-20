package gameanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "gan_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Analytics report for " + task.getInputData().get("gameId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("report", Map.of("gameId", task.getInputData().getOrDefault("gameId","GAME-01"), "kpis", task.getInputData().getOrDefault("kpis", Map.of()), "generatedAt", "2026-03-08", "status", "PUBLISHED"));
        return r;
    }
}
