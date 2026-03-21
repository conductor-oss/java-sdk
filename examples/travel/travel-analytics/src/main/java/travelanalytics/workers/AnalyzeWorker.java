package travelanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "tan_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Identified 3 cost-saving opportunities");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("insights", Map.of("topInsight","Advance booking saves 23% on average","savingsOpportunity",88550,"topDestinations",List.of("New York","San Francisco","Chicago"))); return r;
    }
}
