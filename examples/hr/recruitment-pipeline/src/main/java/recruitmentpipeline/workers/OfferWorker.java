package recruitmentpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class OfferWorker implements Worker {
    @Override public String getTaskDefName() { return "rcp_offer"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [offer] Offer extended to " + task.getInputData().get("candidateName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("offerId", "OFR-602");
        r.getOutputData().put("salary", 145000);
        r.getOutputData().put("startDate", "2024-05-01");
        return r;
    }
}
