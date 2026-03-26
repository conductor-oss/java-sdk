package prescriptionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FillWorkerTest {

    private final FillWorker worker = new FillWorker();

    @Test
    void taskDefName() {
        assertEquals("prx_fill", worker.getTaskDefName());
    }

    @Test
    void fillsWithValidInputs() {
        Task task = taskWith("Amoxicillin", "500mg");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FILL-78901", result.getOutputData().get("fillId"));
        assertEquals(30, result.getOutputData().get("quantity"));
        assertEquals(30, result.getOutputData().get("daysSupply"));
    }

    @Test
    void handlesNullMedicationAndDosage() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fillId"));
    }

    @Test
    void quantityMatchesDaysSupply() {
        Task task = taskWith("Metformin", "850mg");

        TaskResult result = worker.execute(task);

        assertEquals(result.getOutputData().get("quantity"), result.getOutputData().get("daysSupply"));
    }

    private Task taskWith(String medication, String dosage) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("medication", medication);
        input.put("dosage", dosage);
        task.setInputData(input);
        return task;
    }
}
