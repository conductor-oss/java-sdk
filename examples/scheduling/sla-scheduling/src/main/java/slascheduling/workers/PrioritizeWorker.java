package slascheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class PrioritizeWorker implements Worker {
    @Override public String getTaskDefName() { return "sla_prioritize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [prioritize] Prioritizing tickets by SLA (policy: " + task.getInputData().get("slaPolicy") + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("orderedTickets", List.of(Map.of("id","TKT-101","sla","1h","priority",1),Map.of("id","TKT-102","sla","4h","priority",2),Map.of("id","TKT-103","sla","24h","priority",3)));
        r.getOutputData().put("totalTickets", 3);
        r.getOutputData().put("strategy", "sla-urgency-first");
        return r;
    }
}
