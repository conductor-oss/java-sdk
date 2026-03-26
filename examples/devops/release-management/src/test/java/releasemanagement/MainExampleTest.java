package releasemanagement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        assertEquals("rm_prepare", new releasemanagement.workers.PrepareWorker().getTaskDefName());
        assertEquals("rm_approve", new releasemanagement.workers.ApproveWorker().getTaskDefName());
        assertEquals("rm_deploy", new releasemanagement.workers.DeployWorker().getTaskDefName());
        assertEquals("rm_announce", new releasemanagement.workers.AnnounceWorker().getTaskDefName());
    }
}
