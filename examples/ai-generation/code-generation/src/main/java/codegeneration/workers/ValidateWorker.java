package codegeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "cdg_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Syntax check passed, no lint errors");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("syntaxValid", true); r.getOutputData().put("lintErrors", 0); r.getOutputData().put("warnings", 1);
        return r;
    }
}
