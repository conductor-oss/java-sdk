package tracecollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssembleTraceWorker implements Worker {
    @Override public String getTaskDefName() { return "trc_assemble_trace"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assemble_trace] Processing trace task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("traceDepth", 4); r.getOutputData().put("totalDurationMs", 182);
        return r;
    }
}
