package questionanswering.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class RetrieveContextWorker implements Worker {
    @Override public String getTaskDefName() { return "qas_retrieve_context"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [retrieve] Retrieved 3 relevant passages from " + task.getInputData().get("knowledgeBase"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("context", List.of("Conductor workflows support configurable timeouts.", "The timeoutSeconds parameter defines maximum execution time.", "If exceeded, workflow transitions to TIMED_OUT."));
        r.getOutputData().put("sources", List.of("docs/timeouts.md", "docs/workflow-config.md"));
        r.getOutputData().put("relevanceScores", List.of(0.95, 0.88, 0.82));
        return r;
    }
}
