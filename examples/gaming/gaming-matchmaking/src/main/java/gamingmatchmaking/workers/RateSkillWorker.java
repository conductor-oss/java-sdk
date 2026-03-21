package gamingmatchmaking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class RateSkillWorker implements Worker {
    @Override public String getTaskDefName() { return "gmm_rate_skill"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [skill] Rating players by skill");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("ranked", List.of(Map.of("id","P-101","mmr",1520), Map.of("id","P-103","mmr",1510), Map.of("id","P-105","mmr",1490)));
        return result;
    }
}
