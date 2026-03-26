package accountopening.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class CollectInfoWorkerTest {
    private final CollectInfoWorker worker = new CollectInfoWorker();

    @Test void taskDefName() { assertEquals("acc_collect_info", worker.getTaskDefName()); }

    @Test void validApplication() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "123-45-6789", "dateOfBirth", "1990-05-15"));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("nameValid"));
        assertEquals(true, r.getOutputData().get("ssnValid"));
        assertEquals(true, r.getOutputData().get("dobValid"));
        assertNotNull(r.getOutputData().get("auditTrail"));
    }

    @Test void invalidSsnFormat_allZeros() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "000-45-6789"));
        assertEquals(false, r.getOutputData().get("ssnValid"));
    }

    @Test void invalidSsnFormat_wrongPattern() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "12345-6789"));
        assertEquals(false, r.getOutputData().get("ssnValid"));
    }

    @Test void missingSsn() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000));
        assertEquals(false, r.getOutputData().get("ssnValid"));
    }

    @Test void dobUnder18_rejected() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "dateOfBirth", "2020-01-01"));
        assertEquals(false, r.getOutputData().get("dobValid"));
    }

    @Test void dobFutureDate_rejected() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "dateOfBirth", "2030-01-01"));
        assertEquals(false, r.getOutputData().get("dobValid"));
    }

    @Test void failsWithTerminalErrorOnMissingName() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("accountType", "checking", "initialDeposit", 1000)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("applicantName"));
    }

    @Test void failsWithTerminalErrorOnInvalidAccountType() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "bitcoin",
                "initialDeposit", 1000));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid account type"));
    }

    @Test void failsWithTerminalErrorOnInsufficientDeposit() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "money_market",
                "initialDeposit", 100));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Minimum deposit"));
    }

    @Test void auditTrailContainsRequiredFields() {
        TaskResult r = exec(Map.of("applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "123-45-6789", "dateOfBirth", "1990-05-15"));
        Map<String, Object> audit = (Map<String, Object>) r.getOutputData().get("auditTrail");
        assertNotNull(audit.get("timestamp"));
        assertEquals("collect_applicant_info", audit.get("action"));
        assertNotNull(audit.get("nameValidated"));
        assertNotNull(audit.get("ssnFormatValidated"));
    }

    private TaskResult exec(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(input));
        return worker.execute(t);
    }
}
