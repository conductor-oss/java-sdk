package tournamentbracket.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class SeedWorker implements Worker {
    @Override public String getTaskDefName() { return "tbk_seed"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [seed] Seeding players by ranking");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("seeded", List.of(Map.of("seed",1,"player","Alpha"), Map.of("seed",2,"player","Gamma"), Map.of("seed",3,"player","Epsilon"), Map.of("seed",4,"player","Theta")));
        return result;
    }
}
