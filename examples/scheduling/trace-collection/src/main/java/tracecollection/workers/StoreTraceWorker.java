package tracecollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StoreTraceWorker implements Worker {
    @Override public String getTaskDefName() { return "trc_store_trace"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [store_trace] Processing trace task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("stored", true); r.getOutputData().put("storageBackend", "jaeger");
        return r;
    }
}
