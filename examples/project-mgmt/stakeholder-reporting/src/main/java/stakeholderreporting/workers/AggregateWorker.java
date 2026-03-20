package stakeholderreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "shr_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Aggregating team updates");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("summary", Map.of("teamsReporting",3,"onTrack",2,"atRisk",1,"overallHealth","YELLOW")); return r;
    }
}
