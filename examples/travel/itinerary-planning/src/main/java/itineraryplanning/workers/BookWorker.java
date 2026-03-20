package itineraryplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "itp_book"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [book] Flight and hotel booked");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bookingIds", List.of("FLT-544","HTL-544"));
        r.getOutputData().put("totalCost", 1850);
        return r;
    }
}
