package appointmentscheduling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {

    private final ConfirmWorker worker = new ConfirmWorker();

    @Test
    void taskDefName() {
        assertEquals("apt_confirm", worker.getTaskDefName());
    }

    @Test
    void confirmsAppointment() {
        Task task = taskWith(Map.of("patientId", "PAT-001", "appointmentId", "APT-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void sendsViaEmailAndSms() {
        Task task = taskWith(Map.of("patientId", "PAT-001", "appointmentId", "APT-001"));
        TaskResult result = worker.execute(task);

        assertEquals("email+sms", result.getOutputData().get("channel"));
    }

    @Test
    void handlesNullAppointmentId() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-001");
        input.put("appointmentId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void confirmedIsAlwaysTrue() {
        Task task = taskWith(Map.of("patientId", "PAT-999", "appointmentId", "APT-999"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void channelIsDeterministic() {
        Task task1 = taskWith(Map.of("appointmentId", "APT-001"));
        Task task2 = taskWith(Map.of("appointmentId", "APT-002"));

        assertEquals(worker.execute(task1).getOutputData().get("channel"),
                     worker.execute(task2).getOutputData().get("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
