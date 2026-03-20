package tracecollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectSpansWorker implements Worker {
    @Override public String getTaskDefName() { return "trc_collect_spans"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect_spans] Processing trace task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("spanCount", 18);
        return r;
    }
}
