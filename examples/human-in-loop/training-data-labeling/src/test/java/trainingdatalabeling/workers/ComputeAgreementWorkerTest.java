package trainingdatalabeling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeAgreementWorkerTest {

    @Test
    void taskDefName() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        assertEquals("tdl_compute_agreement", worker.getTaskDefName());
    }

    @Test
    void fullAgreement() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("cat", "dog", "bird"),
                "labels2", List.of("cat", "dog", "bird")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("agreements"));
        assertEquals(0, result.getOutputData().get("disagreements"));
        assertEquals(3, result.getOutputData().get("total"));
        assertEquals(100.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void noAgreement() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("cat", "dog", "bird"),
                "labels2", List.of("fish", "snake", "frog")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("agreements"));
        assertEquals(3, result.getOutputData().get("disagreements"));
        assertEquals(3, result.getOutputData().get("total"));
        assertEquals(0.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void partialAgreement() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("cat", "dog", "bird", "fish"),
                "labels2", List.of("cat", "snake", "bird", "frog")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("agreements"));
        assertEquals(2, result.getOutputData().get("disagreements"));
        assertEquals(4, result.getOutputData().get("total"));
        assertEquals(50.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void singleItemAgreement() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("positive"),
                "labels2", List.of("positive")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("agreements"));
        assertEquals(0, result.getOutputData().get("disagreements"));
        assertEquals(1, result.getOutputData().get("total"));
        assertEquals(100.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void singleItemDisagreement() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("positive"),
                "labels2", List.of("negative")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("agreements"));
        assertEquals(1, result.getOutputData().get("disagreements"));
        assertEquals(1, result.getOutputData().get("total"));
        assertEquals(0.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void unequalLengthsUsesMinimum() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("cat", "dog"),
                "labels2", List.of("cat", "dog", "bird")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("agreements"));
        assertEquals(0, result.getOutputData().get("disagreements"));
        assertEquals(2, result.getOutputData().get("total"));
        assertEquals(100.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void emptyLists() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of(),
                "labels2", List.of()
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("agreements"));
        assertEquals(0, result.getOutputData().get("disagreements"));
        assertEquals(0, result.getOutputData().get("total"));
        assertEquals(0.0, result.getOutputData().get("agreementPct"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        ComputeAgreementWorker worker = new ComputeAgreementWorker();
        Task task = taskWith(Map.of(
                "labels1", List.of("a", "b"),
                "labels2", List.of("a", "c")
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("agreements"));
        assertTrue(result.getOutputData().containsKey("disagreements"));
        assertTrue(result.getOutputData().containsKey("total"));
        assertTrue(result.getOutputData().containsKey("agreementPct"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
