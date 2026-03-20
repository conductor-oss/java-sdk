package slascheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ExecuteTasksWorker implements Worker {
    @Override public String getTaskDefName() { return "sla_execute_tasks"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Processing " + task.getInputData().get("totalTickets") + " tickets in SLA priority order");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("results", List.of(Map.of("id","TKT-101","resolved",true,"timeToResolveMin",45),Map.of("id","TKT-102","resolved",true,"timeToResolveMin",180),Map.of("id","TKT-103","resolved",true,"timeToResolveMin",600)));
        r.getOutputData().put("allResolved", true);
        return r;
    }
}
