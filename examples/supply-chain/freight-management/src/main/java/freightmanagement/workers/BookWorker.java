package freightmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "frm_book"; }
    @Override public TaskResult execute(Task task) {
        Object w = task.getInputData().get("weight");
        double weight = w instanceof Number ? ((Number)w).doubleValue() : 0;
        double rate = weight * 2.5;
        System.out.println("  [book] " + task.getInputData().get("carrier") + ": " + task.getInputData().get("origin") + " -> " + task.getInputData().get("destination") + ", " + weight + " kg @ $" + rate);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bookingId", "FRT-667-001"); r.getOutputData().put("rate", rate); return r;
    }
}
