package wearabledata.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessDataWorker implements Worker {
    @Override public String getTaskDefName() { return "wer_process_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("processed", true);
        r.getOutputData().put("avgHeartRate", "avgHR.toFixed(1)");
        return r;
    }
}
