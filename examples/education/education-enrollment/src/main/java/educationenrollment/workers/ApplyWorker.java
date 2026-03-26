package educationenrollment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes student application submission.
 * Input: studentName, program, gpa
 * Output: applicationId
 */
public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edu_apply";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentName = (String) task.getInputData().get("studentName");
        String program = (String) task.getInputData().get("program");

        System.out.println("  [apply] Application from " + studentName + " for " + program);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationId", "APP-671-001");
        return result;
    }
}
