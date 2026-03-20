package incidentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SuggestFixWorker implements Worker {
    @Override public String getTaskDefName() { return "iai_suggest_fix"; }
    @Override public TaskResult execute(Task task) {
        String fix = "Switch to backup payment gateway and increase timeout to 30s";
        System.out.println("  [suggest] Fix: " + fix);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("suggestedFix", fix);
        return result;
    }
}
