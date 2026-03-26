package eventfundraising.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "efr_collect"; }
    @Override public TaskResult execute(Task task) {
        int attendees = 220; int ticketPrice = 150;
        Object a = task.getInputData().get("attendees"); if (a instanceof Number) attendees = ((Number)a).intValue();
        Object tp = task.getInputData().get("ticketPrice"); if (tp instanceof Number) ticketPrice = ((Number)tp).intValue();
        int revenue = attendees * ticketPrice;
        System.out.println("  [collect] Revenue collected: $" + revenue);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("revenue", revenue); r.addOutputData("donations", 8500); r.addOutputData("totalRaised", revenue + 8500); return r;
    }
}
