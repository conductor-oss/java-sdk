package passwordreset.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyTokenWorkerTest {
    @Test void validTokenVerifies() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString())));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("verified"));
    }
}
