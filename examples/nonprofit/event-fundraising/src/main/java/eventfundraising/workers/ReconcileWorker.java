package eventfundraising.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReconcileWorker implements Worker {
    @Override public String getTaskDefName() { return "efr_reconcile"; }
    @Override public TaskResult execute(Task task) {
        int revenue = 33000; int expenses = 12000;
        Object rv = task.getInputData().get("revenue"); if (rv instanceof Number) revenue = ((Number)rv).intValue();
        Object ex = task.getInputData().get("expenses"); if (ex instanceof Number) expenses = ((Number)ex).intValue();
        int net = revenue - expenses;
        System.out.println("  [reconcile] Net raised: $" + net);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("fundraiser", Map.of("eventId", task.getInputData().getOrDefault("eventId","EVT-759"), "revenue", revenue, "expenses", expenses, "netRaised", net, "status", "RECONCILED")); return r;
    }
}
