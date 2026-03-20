package recruitmentpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class PostWorker implements Worker {
    @Override public String getTaskDefName() { return "rcp_post"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [post] Job posted: " + task.getInputData().get("jobTitle") + " in " + task.getInputData().get("department"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("jobId", "JOB-602");
        r.getOutputData().put("platforms", List.of("LinkedIn", "Indeed", "Greenhouse"));
        return r;
    }
}
