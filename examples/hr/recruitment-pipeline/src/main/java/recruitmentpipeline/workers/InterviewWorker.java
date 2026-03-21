package recruitmentpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class InterviewWorker implements Worker {
    @Override public String getTaskDefName() { return "rcp_interview"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [interview] Technical interview with " + task.getInputData().get("candidateName") + ": 4.2/5");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("score", 4.2);
        r.getOutputData().put("rounds", 3);
        r.getOutputData().put("feedback", "Strong problem solver");
        return r;
    }
}
