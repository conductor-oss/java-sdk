package benefitsenrollment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SelectWorker implements Worker {
    @Override public String getTaskDefName() { return "ben_select"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Employee selected: PPO medical, premium dental, standard vision");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("selections", Map.of("medical","PPO","dental","premium","vision","standard","life","2x-salary"));
        return r;
    }
}
