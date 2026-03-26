package tracecollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class InstrumentWorker implements Worker {
    @Override public String getTaskDefName() { return "trc_instrument"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [instrument] Processing trace task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("instrumentedServices", 5); r.getOutputData().put("samplingRate", task.getInputData().get("samplingRate"));
        return r;
    }
}
