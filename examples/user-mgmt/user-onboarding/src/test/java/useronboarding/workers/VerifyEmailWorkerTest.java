package useronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyEmailWorkerTest {
    @Test void taskDefName() { assertEquals("uo_verify_email", new VerifyEmailWorker().getTaskDefName()); }

    @Test void generatesVerificationCode() {
        VerifyEmailWorker w = new VerifyEmailWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "test@example.com")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("verificationSent"));
        assertEquals(6, ((String) r.getOutputData().get("verificationCode")).length());
    }
}
