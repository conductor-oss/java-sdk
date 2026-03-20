package financialaudit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CollectEvidenceWorker implements Worker {
    @Override public String getTaskDefName() { return "fau_collect_evidence"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [evidence] Collecting evidence for scope areas");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("evidenceCount", 47); r.getOutputData().put("documentsReviewed", 120);
        r.getOutputData().put("interviewsConducted", 8); r.getOutputData().put("samplesCollected", 35);
        return r;
    }
}
