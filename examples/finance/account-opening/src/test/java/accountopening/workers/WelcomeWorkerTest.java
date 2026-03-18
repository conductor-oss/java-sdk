package accountopening.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WelcomeWorkerTest {
    private final WelcomeWorker worker = new WelcomeWorker();

    @Test void taskDefName() { assertEquals("acc_welcome", worker.getTaskDefName()); }

    @Test void sendsWelcome() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("applicantName", "Emily", "accountNumber", "ACCT-123", "accountType", "checking")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("welcomeSent"));
    }

    @Test void checkingIncludesDebitCardAndChecks() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("applicantName", "Emily", "accountNumber", "ACCT-123", "accountType", "checking")));
        TaskResult r = worker.execute(t);
        @SuppressWarnings("unchecked")
        List<String> includes = (List<String>) r.getOutputData().get("includes");
        assertTrue(includes.contains("debit_card"));
        assertTrue(includes.contains("checks"));
    }

    @Test void savingsIncludesGoalTools() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("applicantName", "Emily", "accountNumber", "ACCT-123", "accountType", "savings")));
        TaskResult r = worker.execute(t);
        @SuppressWarnings("unchecked")
        List<String> includes = (List<String>) r.getOutputData().get("includes");
        assertTrue(includes.contains("savings_goal_tools"));
    }

    @Test void generatesEmailBody() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("applicantName", "Emily", "accountNumber", "ACCT-123", "accountType", "checking")));
        TaskResult r = worker.execute(t);
        String body = (String) r.getOutputData().get("emailBody");
        assertTrue(body.contains("Emily"));
        assertTrue(body.contains("ACCT-123"));
    }
}
