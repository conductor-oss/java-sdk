package questionanswering.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class GenerateAnswerWorker implements Worker {
    @Override public String getTaskDefName() { return "qas_generate_answer"; }
    @Override public TaskResult execute(Task task) {
        String answer = "You can configure workflow timeouts using the timeoutSeconds parameter in the workflow definition. This defines the maximum execution time. If exceeded, the workflow transitions to TIMED_OUT status.";
        System.out.println("  [answer] Answer generated (" + answer.split(" ").length + " words)");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("answer", answer); r.getOutputData().put("confidence", 0.93); r.getOutputData().put("model", "qa-large-v2");
        return r;
    }
}
