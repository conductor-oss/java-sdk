package abandonedcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class OfferDiscountWorker implements Worker {
    @Override public String getTaskDefName() { return "abc_offer_discount"; }
    @Override public TaskResult execute(Task task) {
        double total = 0;
        Object t = task.getInputData().get("cartTotal");
        if (t instanceof Number) total = ((Number) t).doubleValue();
        int discount = total > 50 ? 15 : 10;
        String code = "SAVE" + discount + "-" + task.getInputData().get("cartId");
        System.out.println("  [discount] Offering " + discount + "% off with code " + code);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("discountCode", code); o.put("discountPercent", discount);
        r.setOutputData(o); return r;
    }
}
