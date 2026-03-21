package documentqa.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ChunkWorker implements Worker {
    @Override public String getTaskDefName() { return "dqa_chunk"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [chunk] Document split into 42 chunks");r.getOutputData().put("chunks", java.util.List.of(java.util.Map.of("id", 0, "text", "Revenue grew 15%")));r.getOutputData().put("chunkCount", 42);r.getOutputData().put("strategy", "semantic");
        return r;
    }
}
