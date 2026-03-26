package liveops.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class DeployWorker implements Worker {
    @Override public String getTaskDefName() { return "lop_deploy"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deploy] Deploying event " + task.getInputData().get("eventId") + " to all servers");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("deployed", true); r.addOutputData("servers", 12); r.addOutputData("regions", List.of("NA","EU","APAC"));
        return r;
    }
}
