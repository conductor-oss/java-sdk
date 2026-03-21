package backgroundcheck.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CriminalWorker implements Worker {
    @Override public String getTaskDefName() { return "bgc_criminal"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [criminal] Criminal record check for " + task.getInputData().get("candidateId") + ": clear");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "clear");
        r.getOutputData().put("jurisdictions", 3);
        return r;
    }
}
