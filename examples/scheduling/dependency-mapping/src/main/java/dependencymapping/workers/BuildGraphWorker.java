package dependencymapping.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class BuildGraphWorker implements Worker {
    @Override public String getTaskDefName() { return "dep_build_graph"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [graph] Building dependency graph");
        r.getOutputData().put("graphId", "dep-graph-20260308");
        r.getOutputData().put("nodes", 8);
        r.getOutputData().put("edgeCount", 14);
        r.getOutputData().put("circularDeps", 0);
        return r;
    }
}
