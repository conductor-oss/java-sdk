package expensereporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CategorizeWorker implements Worker {
    @Override public String getTaskDefName() { return "exr_categorize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [categorize] Processing expense reporting step");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("categorized", Map.of("hotel",890,"meals",245,"transport",180,"misc",35)); r.getOutputData().put("total", 1500); return r; } }
