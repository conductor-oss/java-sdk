package documentqa.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class QueryWorker implements Worker {
    @Override public String getTaskDefName() { return "dqa_query"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [query] Top 3 relevant chunks retrieved");r.getOutputData().put("relevantChunks", java.util.List.of(java.util.Map.of("id", 5, "text", "Total revenue Q4: $2.4B", "score", 0.96)));
        return r;
    }
}
