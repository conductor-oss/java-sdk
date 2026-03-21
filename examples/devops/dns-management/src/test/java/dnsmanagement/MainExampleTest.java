package dnsmanagement;

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
        assertEquals("dns_plan", new dnsmanagement.workers.PlanWorker().getTaskDefName());
        assertEquals("dns_validate", new dnsmanagement.workers.ValidateWorker().getTaskDefName());
        assertEquals("dns_apply", new dnsmanagement.workers.ApplyWorker().getTaskDefName());
        assertEquals("dns_verify", new dnsmanagement.workers.VerifyWorker().getTaskDefName());
    }
}
