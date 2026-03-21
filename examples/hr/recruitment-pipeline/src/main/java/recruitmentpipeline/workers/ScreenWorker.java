package recruitmentpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScreenWorker implements Worker {
    @Override public String getTaskDefName() { return "rcp_screen"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [screen] Resume screening for " + task.getInputData().get("candidateName") + ": 85/100");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("score", 85);
        r.getOutputData().put("passedScreen", true);
        return r;
    }
}
