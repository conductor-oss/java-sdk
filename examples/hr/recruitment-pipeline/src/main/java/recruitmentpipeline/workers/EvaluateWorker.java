package recruitmentpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EvaluateWorker implements Worker {
    @Override public String getTaskDefName() { return "rcp_evaluate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [evaluate] Combined score: screen " + task.getInputData().get("screenScore") + ", interview " + task.getInputData().get("interviewScore"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recommendation", "hire");
        r.getOutputData().put("overallScore", 4.5);
        return r;
    }
}
