package documentationai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class FormatWorker implements Worker {
    @Override public String getTaskDefName() { return "doc_format"; }
    @Override public TaskResult execute(Task task) {
        String fmt = (String) task.getInputData().getOrDefault("outputFormat", "markdown");
        System.out.println("  [format] Formatted docs as " + fmt);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedDocs", task.getInputData().get("rawDocs"));
        result.getOutputData().put("format", fmt);
        return result;
    }
}
