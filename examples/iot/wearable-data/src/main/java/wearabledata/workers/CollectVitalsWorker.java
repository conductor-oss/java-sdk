package wearabledata.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CollectVitalsWorker implements Worker {
    @Override public String getTaskDefName() { return "wer_collect_vitals"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
