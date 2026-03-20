package studentprogress.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectGradesWorker implements Worker {
    @Override public String getTaskDefName() { return "spr_collect_grades"; }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        String semester = (String) task.getInputData().get("semester");
        List<Map<String, Object>> grades = List.of(
                Map.of("course", "CS-101", "grade", "A", "credits", 4),
                Map.of("course", "MATH-201", "grade", "B+", "credits", 3),
                Map.of("course", "ENG-102", "grade", "A-", "credits", 3),
                Map.of("course", "PHYS-101", "grade", "B", "credits", 4));
        System.out.println("  [collect] " + grades.size() + " courses for " + studentId + " in " + semester);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("grades", grades);
        result.getOutputData().put("courseCount", grades.size());
        return result;
    }
}
