package taskassignment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class MatchSkillsWorker implements Worker {
    @Override public String getTaskDefName() { return "tas_match_skills"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [match] Finding best match for skills");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bestMatch", Map.of("name","Alice","score",95,"availability","open")); return r;
    }
}
