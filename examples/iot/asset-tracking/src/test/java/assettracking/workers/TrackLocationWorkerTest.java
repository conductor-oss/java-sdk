package assettracking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackLocationWorkerTest {
    @Test void taskDefName() { assertEquals("ast_track_location", new TrackLocationWorker().getTaskDefName()); }

    @Test void validatesCoordinates() {
        TrackLocationWorker w = new TrackLocationWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("latitude", 37.7749, "longitude", -122.4194)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("validCoords"));
    }
}
