package distributedlogging.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectSvc3Worker implements Worker {
    @Override public String getTaskDefName() { return "dg_collect_svc3"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [" + task.getInputData().get("service") + "] Collecting logs for trace " + task.getInputData().get("traceId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("logCount", 28);
        r.getOutputData().put("service", task.getInputData().get("service"));
        return r;
    }
}
