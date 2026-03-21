package bulkuserimport.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BatchInsertWorker implements Worker {
    @Override public String getTaskDefName() { return "bui_batch_insert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [insert] Batch inserted 1238 users in 13 batches");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("insertedCount", 1238);
        r.getOutputData().put("batches", 13);
        r.getOutputData().put("avgBatchTime", "240ms");
        return r;
    }
}
