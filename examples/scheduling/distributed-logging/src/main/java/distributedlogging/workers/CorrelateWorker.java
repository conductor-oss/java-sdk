package distributedlogging.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CorrelateWorker implements Worker {
    @Override public String getTaskDefName() { return "dg_correlate"; }
    @Override public TaskResult execute(Task task) {
        int s1 = 0, s2 = 0, s3 = 0;
        try { s1 = Integer.parseInt(String.valueOf(task.getInputData().get("svc1Logs"))); } catch (Exception ignored) {}
        try { s2 = Integer.parseInt(String.valueOf(task.getInputData().get("svc2Logs"))); } catch (Exception ignored) {}
        try { s3 = Integer.parseInt(String.valueOf(task.getInputData().get("svc3Logs"))); } catch (Exception ignored) {}
        int total = s1 + s2 + s3;
        System.out.println("  [correlate] Correlating " + total + " logs across 3 services for trace " + task.getInputData().get("traceId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("totalLogs", total);
        r.getOutputData().put("correlatedEvents", 12);
        r.getOutputData().put("traceId", task.getInputData().get("traceId"));
        r.getOutputData().put("requestFlow", List.of("api-gateway", "order-service", "payment-service"));
        return r;
    }
}
