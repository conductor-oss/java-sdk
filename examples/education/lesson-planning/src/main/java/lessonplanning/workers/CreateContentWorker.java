package lessonplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CreateContentWorker implements Worker {
    @Override public String getTaskDefName() { return "lpl_create_content"; }

    @Override
    public TaskResult execute(Task task) {
        String lessonTitle = (String) task.getInputData().get("lessonTitle");
        List<String> sections = List.of("Introduction", "Theory", "Live Coding", "Practice", "Assessment");
        Map<String, Object> lessonPlan = Map.of(
                "title", lessonTitle != null ? lessonTitle : "Untitled",
                "sections", sections,
                "duration", "90 min");
        System.out.println("  [content] Created " + sections.size() + " sections (90 min)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("lessonPlan", lessonPlan);
        result.getOutputData().put("sectionCount", sections.size());
        return result;
    }
}
