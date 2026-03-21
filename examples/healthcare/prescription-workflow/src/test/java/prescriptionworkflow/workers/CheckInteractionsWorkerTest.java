package prescriptionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CheckInteractionsWorkerTest {

    private final CheckInteractionsWorker worker = new CheckInteractionsWorker();

    @Test
    void taskDefName() {
        assertEquals("prx_check_interactions", worker.getTaskDefName());
    }

    @Test
    void clearedWithCurrentMedications() {
        Task task = taskWith("Amoxicillin", List.of("Lisinopril 10mg", "Metformin 500mg"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cleared"));
        assertEquals("none", result.getOutputData().get("severity"));
    }

    @Test
    void interactionsListIsEmpty() {
        Task task = taskWith("Ibuprofen", List.of("Aspirin 81mg"));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<?> interactions = (List<?>) result.getOutputData().get("interactions");
        assertNotNull(interactions);
        assertTrue(interactions.isEmpty());
    }

    @Test
    void handlesNullMedication() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("currentMedications", List.of("Lisinopril 10mg"));
        // medication is null
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cleared"));
    }

    @Test
    void handlesNullCurrentMedications() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("medication", "Amoxicillin");
        // currentMedications is null
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cleared"));
    }

    private Task taskWith(String medication, List<String> currentMedications) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("medication", medication);
        input.put("currentMedications", currentMedications);
        task.setInputData(input);
        return task;
    }
}
