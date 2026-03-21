package networksegmentation;

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
        assertEquals("ns_define_zones", new networksegmentation.workers.DefineZonesWorker().getTaskDefName());
        assertEquals("ns_configure_rules", new networksegmentation.workers.ConfigureRulesWorker().getTaskDefName());
        assertEquals("ns_apply_policies", new networksegmentation.workers.ApplyPoliciesWorker().getTaskDefName());
        assertEquals("ns_verify_isolation", new networksegmentation.workers.VerifyIsolationWorker().getTaskDefName());
    }
}
