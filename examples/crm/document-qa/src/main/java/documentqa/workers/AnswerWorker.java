package documentqa.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AnswerWorker implements Worker {
    @Override public String getTaskDefName() { return "dqa_answer"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        String answer = "Q4 total revenue was $2.4 billion, a 15% YoY increase.";System.out.println("  [answer] Answer generated");r.getOutputData().put("answer", answer);r.getOutputData().put("confidence", 0.95);r.getOutputData().put("sourceChunks", java.util.List.of(5, 8, 12));
        return r;
    }
}
