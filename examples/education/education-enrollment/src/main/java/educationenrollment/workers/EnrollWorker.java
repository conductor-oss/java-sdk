package educationenrollment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enrolls an admitted student in their program.
 * Input: studentId, program
 * Output: enrolled, semester
 */
public class EnrollWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edu_enroll";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        String program = (String) task.getInputData().get("program");

        System.out.println("  [enroll] " + studentId + " enrolled in " + program);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrolled", true);
        result.getOutputData().put("semester", "Fall 2024");
        return result;
    }
}
