package lessonplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DefineObjectivesWorker implements Worker {
    @Override public String getTaskDefName() { return "lpl_define_objectives"; }

    @Override
    public TaskResult execute(Task task) {
        String lessonTitle = (String) task.getInputData().get("lessonTitle");
        List<String> objectives = List.of(
                "Understand binary search tree operations",
                "Implement insertion and deletion",
                "Analyze time complexity of BST operations");
        System.out.println("  [objectives] Defined " + objectives.size() + " learning objectives for \"" + lessonTitle + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("objectives", objectives);
        result.getOutputData().put("objectiveCount", objectives.size());
        return result;
    }
}
