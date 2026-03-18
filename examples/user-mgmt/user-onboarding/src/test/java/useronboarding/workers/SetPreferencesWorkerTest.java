package useronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SetPreferencesWorkerTest {
    @Test void setsDefaultPreferences() {
        SetPreferencesWorker w = new SetPreferencesWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of()));
        TaskResult r = w.execute(t);
        @SuppressWarnings("unchecked")
        Map<String, Object> prefs = (Map<String, Object>) r.getOutputData().get("preferences");
        assertEquals("en", prefs.get("language"));
    }
}
