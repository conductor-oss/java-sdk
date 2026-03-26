package prescriptionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyWorkerTest {

    private final VerifyWorker worker = new VerifyWorker();

    @Test
    void taskDefName() {
        assertEquals("prx_verify", worker.getTaskDefName());
    }

    @Test
    void verifiesValidPrescription() {
        Task task = taskWith("RX-12345", "PAT-001", "Amoxicillin", "500mg");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals("Dr. Williams", result.getOutputData().get("prescriber"));
    }

    @Test
    void returnsCurrentMedications() {
        Task task = taskWith("RX-12345", "PAT-001", "Ibuprofen", "200mg");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> meds = (List<String>) result.getOutputData().get("currentMedications");
        assertNotNull(meds);
        assertEquals(2, meds.size());
        assertTrue(meds.contains("Lisinopril 10mg"));
        assertTrue(meds.contains("Metformin 500mg"));
    }

    @Test
    void handlesNullInputsGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    private Task taskWith(String prescriptionId, String patientId, String medication, String dosage) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("prescriptionId", prescriptionId);
        input.put("patientId", patientId);
        input.put("medication", medication);
        input.put("dosage", dosage);
        task.setInputData(input);
        return task;
    }
}
