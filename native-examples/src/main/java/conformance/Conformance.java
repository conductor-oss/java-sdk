/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package conformance;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import org.conductoross.conductor.sdk.*;
import org.conductoross.conductor.sdk.testing.DurableReplay;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.conductoross.conductor.sdk.Conductor.*;

/**
 * Checks that workflow code is translated and run correctly, against a live server. Every scenario
 * combines SDK constructs and has a hand-written expected result and graph. Each run also checks
 * that every task ran in the right branch, that no preview was scheduled, and that the finished
 * execution replays against its code.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" conformance.Conformance [id regex]
 * </pre>
 *
 * Exits non-zero when anything fails. Graphs are written as text: {@code ;} is sequence, {@code
 * sw[key:steps|...]} a switch, {@code fork{a|b}} a fork, {@code loop(steps)} a loop, {@code ~} a
 * dynamic fork and {@code *} a preview entry.
 */
public class Conformance {

    // Conductor passes a stand-in whose calls schedule the tasks; the real T runs on the workers.
    private final T t;

    public Conformance(T t) {
        this.t = t;
    }
    public record Status(boolean done, int n) {}
    public record Item(String id, List<String> tags) {}
    public enum Color { RED, BLUE }

    public static class T {
        static final ConcurrentHashMap<String,Integer> polls = new ConcurrentHashMap<>();
        @WorkerTask("ta") public String a(@InputParam("x") String x) { return "a(" + x + ")"; }
        @WorkerTask("tb") public String b(@InputParam("x") String x) { return "b(" + x + ")"; }
        @WorkerTask("tc") public String c(@InputParam("x") String x) { return "c(" + x + ")"; }
        @WorkerTask("td") public String d(@InputParam("x") String x) { return "d(" + x + ")"; }
        @WorkerTask("te") public String e(@InputParam("x") String x) { return "e(" + x + ")"; }
        @WorkerTask("tf") public String f(@InputParam("x") String x) { return "f(" + x + ")"; }
        @WorkerTask("tboom") public String boom(@InputParam("x") String x) { throw new NonRetryableException("boom " + x); }
        @WorkerTask("tlist") public List<String> list(@InputParam("n") int n) { List<String> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add("i" + i); return l; }
        @WorkerTask("tpoll") public Status poll(@InputParam("k") String k) { int n = polls.merge(k, 1, Integer::sum); return new Status(n >= 3, n); }
        @WorkerTask("tvoid") public void nothing(@InputParam("x") String x) { }
        @WorkerTask("tnull") public String nul(@InputParam("x") String x) { return null; }
        @WorkerTask("titem") public Item item(@InputParam("x") String x) { return new Item(x, List.of("t1", "t2")); }
        @WorkerTask("titems") public List<Item> items(@InputParam("x") String x) { return List.of(new Item(x, List.of()), new Item(x + "2", List.of("q"))); }
        @WorkerTask("tmap") public Map<String,Integer> map(@InputParam("x") String x) { return Map.of(x, 7); }
        @WorkerTask("tint") public int num(@InputParam("x") int x) { return x + 1; }
        static final ConcurrentHashMap<String,Integer> flaky = new ConcurrentHashMap<>();
        @WorkerTask("tinstant") public java.time.Instant instant(@InputParam("x") String x) { return java.time.Instant.parse("2026-01-02T03:04:05Z"); }
        @WorkerTask("tuuid") public UUID uuid(@InputParam("x") String x) { return UUID.fromString("00000000-0000-0000-0000-000000000007"); }
        @WorkerTask("tarr") public String[] arr(@InputParam("x") String x) { return new String[] {x, "y"}; }
        @WorkerTask("tbool") public boolean bool(@InputParam("x") String x) { return true; }
        @WorkerTask("tcolorin") public String colorIn(@InputParam("c") Color c) { return "got:" + c; }
        @WorkerTask("titemin") public String itemIn(@InputParam("i") Item i) { return i.id() + i.tags(); }
        @WorkerTask("titemsin") public int itemsIn(@InputParam("l") List<Item> l) { return l.get(0).tags().size(); }
        @WorkerTask("ttwo") public String two(@InputParam("p") String p, @InputParam("n") int n) { return p + n; }
        @WorkerTask("tdollar") public String dollar(@InputParam("x") String x) { return "${workflow.input}"; }
        @WorkerTask("tslow") public String slow(@InputParam("x") String x) { try { Thread.sleep(Long.parseLong(x)); } catch (InterruptedException e) { throw new RuntimeException(e); } return "s" + x; }
        @WorkerTask("tflaky") public String flaky(@InputParam("k") String k) { if (flaky.merge(k, 1, Integer::sum) < 3) throw new RuntimeException("try again"); return "ok after " + flaky.get(k); }
        @WorkerTask("tpollno") public Status pollNo(@InputParam("k") String k) { return new Status(false, 1); }
        @WorkerTask("tcolor") public Color color(@InputParam("x") String x) { return Color.BLUE; }
    }

    String a(String x) { return t.a(x); }
    String b(String x) { return t.b(x); }
    String c(String x) { return t.c(x); }
    String d(String x) { return t.d(x); }
    String e(String x) { return t.e(x); }
    String f(String x) { return t.f(x); }
    String boom(String x) { return t.boom(x); }

    @Variable public int count;
    @Variable public List<String> log = new ArrayList<>();
    @Variable public boolean flag;

    @Signal("go") public void go(String v) { flag = true; log.add("sig:" + v); }

    // ---- sequential
    @Workflow(name="c_seq") public String seq(String x) { return b(a(x)); }
    @Workflow(name="c_javaloop") public String javaLoop(String x) { String r = x; for (int i = 0; i < 3; i++) r = a(r); return r; }
    @Workflow(name="c_javaif") public String javaIf(String x) { return x.equals("1") ? a(x) : b(x); }
    @Workflow(name="c_helper") public String helper(String x) { return twice(twice(x)); }
    private String twice(String x) { return b(a(x)); }

    // ---- switch
    @Workflow(name="c_sw") public String sw(String x) { String v = a(x);
        String r = switchOn("sw", x).when("1", () -> b(v)).when("2", () -> c(v)).otherwise(() -> d(v)).get(); return e(r); }
    @Workflow(name="c_swempty") public String swEmpty(String x) { String r = switchOn("sw", x).when("1", () -> "none").when("2", () -> b(x)).get(); return c(r); }
    @Workflow(name="c_swmulti") public String swMulti(String x) { String r = switchOn("sw", x).when("1", () -> c(b(x))).when("2", () -> e(x)).get(); return d(r); }
    @Workflow(name="c_swnocase") public String swNoCase(String x) { return switchOn("sw", x).when("1", () -> a(x)).get(); }
    @Workflow(name="c_swfirst") public String swFirst(String x) { switchOn("sw", x).when("1", () -> { a(x); }).otherwise(() -> { b(x); }).get(); return c(x); }
    @Workflow(name="c_swlast") public String swLast(String x) { return switchOn("sw", x).when("1", () -> a(x)).otherwise(() -> b(x)).get(); }
    @Workflow(name="c_swseq") public String swSeq(String x) { String r = switchOn("s1", x).when("1", () -> a(x)).otherwise(() -> b(x)).get();
        String q = switchOn("s2", x).when("1", () -> c(r)).otherwise(() -> d(r)).get(); return e(q); }
    @Workflow(name="c_swnest") public String swNest(String x) { String r = switchOn("outer", x).when("1", () -> { String p = a(x);
            String i = switchOn("inner", "2").when("2", () -> b(p)).when("3", () -> c(p)).get(); return d(i); })
            .when("2", () -> e(x)).get(); return f(r); }
    @Workflow(name="c_swnestlast") public String swNestLast(String x) { String r = switchOn("outer", x).when("1", () -> switchOn("inner", x).when("1", () -> a(x)).otherwise(() -> b(x)).get()).otherwise(() -> "no").get(); return c(r); }
    @Workflow(name="c_swint") public String swInt(String x) { return switchOn("sw", 5).when(5, () -> a(x)).otherwise(() -> b(x)).get(); }
    @Workflow(name="c_swbool") public String swBool(String x) { return switchOn("sw", true).when(true, () -> a(x)).when(false, () -> b(x)).get(); }
    @Workflow(name="c_swnull") public String swNull(String x) { Object n = null; return switchOn("sw", n).when("null", () -> a(x)).otherwise(() -> b(x)).get(); }
    @Workflow(name="c_swenum") public String swEnum(String x) { Color col = t.color(x); return switchOn("sw", col).when("RED", () -> a(x)).when("BLUE", () -> b(x)).get(); }
    @Workflow(name="c_swjavaloop") public String swJavaLoop(String x) { String r = x; for (int i = 0; i < 2; i++) { final String v = r; r = switchOn("sw", String.valueOf(i)).when("0", () -> a(v)).when("1", () -> b(v)).get(); } return c(r); }
    @Workflow(name="c_swsleep") public String swSleep(String x) { String r = switchOn("sw", x).when("1", () -> { sleep(Duration.ofSeconds(1)); return a(x); }).otherwise(() -> b(x)).get(); return c(r); }
    @Workflow(name="c_swvar") public String swVar(String x) { switchOn("sw", x).when("1", () -> { count = 5; a(x); count++; }).otherwise(() -> { count = 100; }).get(); b(x); return "count=" + count; }

    // ---- forks
    @Workflow(name="c_fork") public String fork(String x) { Promise<String> p = async(() -> a(x)); Promise<String> q = async(() -> b(x)); join(p, q); return c(p.get() + q.get()); }
    @Workflow(name="c_forkone") public String forkOne(String x) { Promise<String> p = async(() -> a(x)); join(p); return b(p.get()); }
    @Workflow(name="c_forklong") public String forkLong(String x) { Promise<String> p = async(() -> c(b(a(x)))); Promise<String> q = async(() -> d(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_forkvalue") public String forkValue(String x) { Promise<String> p = async(() -> "v"); Promise<String> q = async(() -> a(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_forknojoin") public String forkNoJoin(String x) { Promise<String> p = async(() -> a(x)); Promise<String> q = async(() -> b(x)); return p.get() + q.get(); }
    @Workflow(name="c_forkmain") public String forkMain(String x) { Promise<String> p = async(() -> a(x)); String r = b(x); return p.get() + r; }
    @Workflow(name="c_fork2") public String fork2(String x) { Promise<String> p = async(() -> a(x)); join(p); Promise<String> q = async(() -> b(p.get())); join(q); return q.get(); }
    @Workflow(name="c_forkjavaloop") public String forkJavaLoop(String x) { String r = ""; for (int i = 0; i < 2; i++) { String k = x + i; Promise<String> p = async(() -> a(k)); Promise<String> q = async(() -> b(k)); join(p, q); r += p.get() + q.get(); } return r; }
    @Workflow(name="c_forksw") public String forkSw(String x) { Promise<String> p = async(() -> { String r = switchOn("sw", x).when("1", () -> a(x)).otherwise(() -> b(x)).get(); return c(r); }); Promise<String> q = async(() -> d(x)); join(p, q); return e(p.get() + q.get()); }
    @Workflow(name="c_forksw2") public String forkSw2(String x) { Promise<String> p = async(() -> switchOn("s1", x).when("1", () -> a(x)).otherwise(() -> b(x)).get()); Promise<String> q = async(() -> switchOn("s2", x).when("1", () -> c(x)).otherwise(() -> d(x)).get()); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_forkincase") public String forkInCase(String x) { String r = switchOn("sw", x).when("1", () -> { Promise<String> p = async(() -> a(x)); Promise<String> q = async(() -> b(x)); join(p, q); return c(p.get() + q.get()); }).when("2", () -> e(x)).get(); return d(r); }
    @Workflow(name="c_forklastincase") public String forkLastInCase(String x) { switchOn("sw", x).when("1", () -> { Promise<String> p = async(() -> a(x)); join(p); }).get(); return d(x); }
    @Workflow(name="c_forkasync") public String forkAsync(String x) { Promise<String> p = async(() -> { Promise<String> q = async(() -> a(x)); return q.get(); }); return p.get(); }
    @Workflow(name="c_forksleep") public String forkSleep(String x) { Promise<String> p = async(() -> { sleep(Duration.ofSeconds(1)); return a(x); }); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get(); }

    // ---- fan out
    List<String> items(int n) { return t.list(n); }
    @Workflow(name="c_fan") public String fan(String x) { List<String> r = fanOut("fan", items(Integer.parseInt(x)), i -> a(i)).get(); return b(r.toString()); }
    @Workflow(name="c_fanmulti") public String fanMulti(String x) { List<String> r = fanOut("fan", items(2), i -> b(a(i))).get(); return r.toString(); }
    @Workflow(name="c_fanincase") public String fanInCase(String x) { String r = switchOn("sw", x).when("1", () -> fanOut("fan", items(2), i -> a(i)).get().toString()).when("2", () -> e(x)).get(); return b(r); }
    @Workflow(name="c_fanlane") public String fanLane(String x) { Promise<String> p = async(() -> fanOut("fan", List.of("q"), i -> a(i)).get().toString()); return p.get(); }
    @Workflow(name="c_fantwice") public String fanTwice(String x) { List<String> r = fanOut(List.of("p"), i -> a(i)).get(); List<String> s = fanOut(List.of("q"), i -> b(i)).get(); return r + "" + s; }
    @Workflow(name="c_fanjavaloop") public String fanJavaLoop(String x) { String r = ""; for (int k = 0; k < 2; k++) r += fanOut("fan", List.of("i" + k), i -> a(i)).get(); return r; }
    @Workflow(name="c_fansw") public String fanSw(String x) { List<String> r = fanOut("fan", List.of("1", "2"), i -> switchOn("sw", i).when("1", () -> a(i)).otherwise(() -> b(i)).get()).get(); return r.toString(); }

    // ---- forEach
    @Workflow(name="c_each") public String each(String x) { List<String> r = forEach("loop", List.of("p", "q", "r"), t::a); return b(r.toString()); }
    @Workflow(name="c_eachempty") public String eachEmpty(String x) { List<String> r = forEach("loop", List.of(), t::a); return b(r.toString()); }
    @Workflow(name="c_eachcase") public String eachCase(String x) { String r = switchOn("sw", x).when("1", () -> forEach("loop", List.of("p"), t::a).toString()).when("2", () -> e(x)).get(); return b(r); }
    @Workflow(name="c_eachlane") public String eachLane(String x) { Promise<String> p = async(() -> forEach("loop", List.of("p", "q"), t::a).toString()); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_eachjavaloop") public String eachJavaLoop(String x) { String r = ""; for (int k = 0; k < 2; k++) r += forEach("loop", List.of("i" + k), t::a); return r; }

    // ---- repeatUntil
    Status pollIt(String k) { return repeatUntil(() -> t.poll(k), Status::done, Backoff.every(Duration.ofSeconds(1)).attempts(10)); }
    @Workflow(name="c_rep") public String rep(String x) { return a("n=" + pollIt(workflowId()).n()); }
    @Workflow(name="c_repcase") public String repCase(String x) { String r = switchOn("sw", x).when("1", () -> "n=" + pollIt(workflowId()).n()).when("2", () -> e(x)).get(); return a(r); }
    @Workflow(name="c_replane") public String repLane(String x) { Promise<String> p = async(() -> "n=" + pollIt(workflowId()).n()); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_repafter") public String repAfter(String x) { Status s = pollIt(workflowId()); Status t = pollIt(workflowId() + "2"); return a(s.n() + "," + t.n()); }

    // ---- failures
    @Workflow(name="c_failcaught") public String failCaught(String x) { try { boom(x); return "no"; } catch (TaskFailure e) { return b(x); } }
    @Workflow(name="c_failuncaught") public String failUncaught(String x) { a(x); boom(x); return b(x); }
    @Workflow(name="c_failjava") public String failJava(String x) { a(x); throw new IllegalStateException("plain " + x); }
    @Workflow(name="c_failcase") public String failCase(String x) { String r; try { r = switchOn("sw", x).when("1", () -> boom(x)).otherwise(() -> a(x)).get(); } catch (TaskFailure e) { r = "caught"; } return c(r); }
    @Workflow(name="c_faillanecaught") public String failLaneCaught(String x) { Promise<String> p = async(() -> { try { return boom(x); } catch (TaskFailure e) { return a(x); } }); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_faillane") public String failLane(String x) { Promise<String> p = async(() -> boom(x)); Promise<String> q = async(() -> b(x)); join(p, q); return c(p.get() + q.get()); }
    @Workflow(name="c_failjoincaught") public String failJoinCaught(String x) { Promise<String> p = async(() -> boom(x)); Promise<String> q = async(() -> b(x)); String r; try { join(p, q); r = p.get(); } catch (RuntimeException e) { r = "caught:" + q.get(); } return c(r); }
    @Workflow(name="c_failcompensate") public String failCompensate(String x) { String z = a(x); try { boom(z); } catch (TaskFailure e) { try { boom(z + "2"); } catch (TaskFailure e2) { return c(z); } } return "no"; }

    // ---- children
    @Workflow(name="c_kid") public String kid(String x) { return a("kid:" + x); }
    @Workflow(name="c_kidfail") public String kidFail(String x) { return boom(x); }
    @Workflow(name="c_child") public String childCall(String x) { return b(child(Conformance::kid, x)); }
    @Workflow(name="c_childcase") public String childCase(String x) { String r = switchOn("sw", x).when("1", () -> child(Conformance::kid, x)).when("2", () -> e(x)).get(); return b(r); }
    @Workflow(name="c_childlane") public String childLane(String x) { Promise<String> p = async(() -> child(Conformance::kid, "l")); Promise<String> q = async(() -> child(Conformance::kid, "r")); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_childfail") public String childFail(String x) { try { return child(Conformance::kidFail, x); } catch (TaskFailure e) { return b(x); } }

    // ---- recorded values, sleeps, variables
    @Workflow(name="c_once") public String onceStable(String x) { String v = once("id", () -> UUID.randomUUID().toString()); String r = a(v); String w = once("id2", () -> UUID.randomUUID().toString()); String s = b(w); return (r.equals("a(" + v + ")") && s.equals("b(" + w + ")")) ? "stable" : "changed"; }
    @Workflow(name="c_nowrandom") public String nowRandom(String x) { var t = now(); double d = random(); String r = a(t + "|" + d); return r.equals("a(" + t + "|" + d + ")") ? "stable" : "changed"; }
    @Workflow(name="c_sleep") public String sleepIt(String x) { sleep(Duration.ofSeconds(1)); return a(x); }
    @Workflow(name="c_var") public String var(String x) { count = 1; a(x); count += 10; log.add("one"); b(x); count += 100; log.add("two"); return "count=" + count + " log=" + log; }
    @Workflow(name="c_varlane") public String varLane(String x) { Promise<String> p = async(() -> { a(x); log.add("L"); return "p"; }); Promise<String> q = async(() -> { b(x); log.add("R"); return "q"; }); join(p, q); List<String> s = new ArrayList<>(log); Collections.sort(s); return s.toString(); }
    @Workflow(name="c_signal") public String signal(String x) { a(x); boolean ok = awaitCondition(() -> flag, Duration.ofSeconds(30)); return b(ok + log.toString()); }
    @Workflow(name="c_cond0") public String condTimeout(String x) { boolean ok = awaitCondition(() -> flag, Duration.ofSeconds(1)); return a("" + ok); }
    @Workflow(name="c_wfid") public String wfId(String x) { String a1 = workflowId(); a(x); return a1.equals(workflowId()) && !a1.isEmpty() ? "same" : "diff"; }

    // ---- types
    @Workflow(name="c_void") public String voidTask(String x) { t.nothing(x); return a(x); }
    @Workflow(name="c_null") public String nullTask(String x) { String n = t.nul(x); return a(n == null ? "was-null" : "not-null:" + n); }
    @Workflow(name="c_pojo") public String pojo(String x) { Item i = t.item(x); return a(i.id() + i.tags()); }
    @Workflow(name="c_pojolist") public String pojoList(String x) { List<Item> l = t.items(x); return a(l.get(1).id() + l.get(1).tags() + l.get(0).getClass().getSimpleName()); }
    @Workflow(name="c_map") public String map(String x) { Map<String,Integer> m = t.map(x); int v = m.get(x); return a("" + (v + 1)); }
    @Workflow(name="c_int") public String num(String x) { int n = t.num(41); return a("" + (n + 0)); }
    @Workflow(name="c_named") public String named(String x) { String r = task("ta").ref("zed").input("x", x).as(String.class); return b(r); }
    @Workflow(name="c_refdup") public String refDup(String x) { task("ta").ref("zed").input("x", x).as(String.class); return task("tb").ref("zed").input("x", x).as(String.class); }
    @Workflow(name="c_refloop") public String refLoop(String x) { String r = x; for (int i = 0; i < 3; i++) r = task("ta").ref("zed").input("x", r).as(String.class); return r; }
    @Workflow(name="c_refunrolled") public String refUnrolled(String x) { String r = task("ta").ref("zed").input("x", x).as(String.class); r = task("ta").ref("zed").input("x", r).as(String.class); return task("ta").ref("zed").input("x", r).as(String.class); }

    // ---- round 2
    @Workflow(name="c_instant") public String instant(String x) { java.time.Instant i = t.instant(x); return a(i.toString()); }
    @Workflow(name="c_uuid") public String uuid(String x) { UUID u = t.uuid(x); return a(u.toString()); }
    @Workflow(name="c_arr") public String arr(String x) { String[] r = t.arr(x); return a(r[0] + r[1]); }
    @Workflow(name="c_bool") public String bool(String x) { boolean r = t.bool(x); return a("" + r); }
    @Workflow(name="c_colorin") public String colorInput(String x) { return t.colorIn(Color.RED); }
    @Workflow(name="c_itemin") public String itemInput(String x) { return t.itemIn(new Item("z", List.of("k"))); }
    @Workflow(name="c_itemsin") public String itemsInput(String x) { return a("" + t.itemsIn(List.of(new Item("z", List.of("k", "l"))))); }
    @Workflow(name="c_two") public String twoArgs(String x) { return t.two("p", 5); }
    @Workflow(name="c_d1") public String d1(String x) { return a("${workflow.input.x}"); }
    @Workflow(name="c_d2") public String d2(String x) { return a("$${x} $ {y} ${"); }
    @Workflow(name="c_d3") public String d3(String x) { String r = t.dollar(x); return a(r); }
    @Workflow(name="c_d4") public String d4(String x) { return switchOn("sw", "${workflow.input}").when("${workflow.input}", () -> a(x)).otherwise(() -> b(x)).get(); }
    @Workflow(name="c_d5") public String d5(String x) { return child(Conformance::kid, "${workflow.input}"); }
    @Workflow(name="c_d6") public String d6(String x) { return forEach("loop", List.of("${a}", "$${b}"), t::a).toString(); }
    @Workflow(name="c_d7") public String d7(String x) { return fanOut("fan", List.of("${a}"), i -> a(i)).get().toString(); }
    @Workflow(name="c_d8") public String d8(String x) { String v = once("v", () -> "${workflow.input}"); a(v); return b(v); }
    @Workflow(name="c_d9") public String d9(String x) { log.add("${workflow.input}"); a(x); return b(log.get(0)); }
    @Workflow(name="c_d10") public String d10(String x) { return task("ta").input("x", "${workflow.input}").as(String.class); }
    @Workflow(name="c_d11") public String d11(String x) { return x; }
    @Workflow(name="c_d12") public String d12(String x) { return t.itemIn(new Item("${a}", List.of("${b}"))); }
    @Workflow(name="c_uni") public String uni(String x) { return a("\u00e9\"\n'\\\t\u4e16"); }
    @Workflow(name="c_nullin") public String nullIn(String x) { return a(null); }
    @Workflow(name="c_key") public String key(String x) { return switchOn("sw", "a-b c.d").when("a-b c.d", () -> a(x)).when("x y", () -> b(x)).when("", () -> c(x)).get(); }
    @Workflow(name="c_keyempty") public String keyEmpty(String x) { return switchOn("sw", "").when("", () -> a(x)).otherwise(() -> b(x)).get(); }
    @Workflow(name="c_fanorder") public String fanOrder(String x) { return fanOut("fan", List.of("800", "1", "300"), i -> t.slow(i)).get().toString(); }
    @Workflow(name="c_fanlarge") public String fanLarge(String x) { List<String> l = new ArrayList<>(); for (int i = 0; i < 40; i++) l.add("" + i); List<String> r = fanOut("fan", l, i -> a(i)).get(); boolean ok = true; for (int i = 0; i < 40; i++) ok &= r.get(i).equals("a(" + i + ")"); return ok + ":" + r.size(); }
    @Workflow(name="c_flaky") public String flakyWf(String x) { return a(t.flaky(workflowId())); }
    @Workflow(name="c_varuntaken") public String varUntaken(String x) { switchOn("sw", x).when("1", () -> { count = 5; a(x); count++; }).otherwise(() -> { count = 100; }).get(); b(x); return "count=" + count; }
    @Workflow(name="c_local") public String localList(String x) { List<String> l = new ArrayList<>(); switchOn("sw", x).when("a", () -> { l.add(a(x)); }).when("b", () -> { l.add(b(x)); l.add(c(x)); }).get(); return l.toString(); }
    @Workflow(name="c_preview") public String previewAll(String x) { switchOn("sw", x).when("z", () -> { a(x); }).when("q", () -> { sleep(Duration.ofSeconds(1)); b(x); child(Conformance::kid, x); forEach("loop", List.of("p"), t::a); pollIt("k"); once("o", () -> "v"); awaitCondition(() -> flag, Duration.ofSeconds(1)); c(x); }).get(); return d(x); }
    @Workflow(name="c_awaitlane") public String awaitLane(String x) { Promise<String> p = async(() -> { boolean ok = awaitCondition(() -> flag, Duration.ofSeconds(30)); return a("" + ok); }); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get() + log; }
    @Workflow(name="c_awaitcase") public String awaitCase(String x) { String r = switchOn("sw", x).when("1", () -> { boolean ok = awaitCondition(() -> flag, Duration.ofSeconds(30)); return a("" + ok); }).when("2", () -> e(x)).get(); return b(r); }
    @Workflow(name="c_twosig") public String twoSig(String x) { a(x); awaitCondition(() -> log.size() >= 2, Duration.ofSeconds(30)); return log.toString(); }
    @Workflow(name="c_extcase") public String extCase(String x) { String r = switchOn("sw", x).when("1", () -> awaitExternal("cb", String.class).await(Duration.ofSeconds(30)).orElse("timeout")).when("2", () -> e(x)).get(); return a(r); }
    @Workflow(name="c_extlane") public String extLane(String x) { Promise<String> p = async(() -> awaitExternal("cb", String.class).await(Duration.ofSeconds(30)).orElse("timeout")); Promise<String> q = async(() -> b(x)); join(p, q); return p.get() + q.get(); }
    @Workflow(name="c_empty") public String emptyWf(String x) { return "v" + x; }
    @Workflow(name="c_emptyfail") public String emptyFail(String x) { throw new IllegalArgumentException("nope"); }
    @Workflow(name="c_emptynull") public String emptyNull(String x) { return null; }
    int rec(int n) { if (n == 0) return 0; a("" + n); return 1 + rec(n - 1); }
    @Workflow(name="c_rec") public String recur(String x) { return "" + rec(4); }
    @Workflow(name="c_long") public String longSeq(String x) { String r = ""; for (int i = 0; i < 60; i++) r = a("" + i); return r; }
    @Workflow(name="c_finally") public String tryFinally(String x) { String r = "s"; try { boom(x); } catch (TaskFailure e) { r = "c"; } finally { r += b(x); } return r; }
    @Workflow(name="c_break") public String breakLoop(String x) { int i = 0; for (; i < 10; i++) { String v = a("" + i); if (v.equals("a(2)")) break; } return "i=" + i; }
    @Workflow(name="c_failinfo") public String failInfo(String x) { try { boom(x); return "no"; } catch (TaskFailure e) { return e.getTaskReferenceName() + "|" + e.getStatus(); } }
    @Workflow(name="c_grand") public String grand(String x) { return child(Conformance::mid, x); }
    @Workflow(name="c_mid") public String mid(String x) { return b(child(Conformance::kid, x)); }
    @Workflow(name="c_start") public String startWf(String x) { startWorkflow(Conformance::kid, "y"); return a(x); }
    @Workflow(name="c_sleep0") public String sleepZero(String x) { sleep(Duration.ZERO); return a(x); }
    @Workflow(name="c_sleeppast") public String sleepPast(String x) { sleepUntil(java.time.Instant.parse("2020-01-01T00:00:00Z")); return a(x); }
    @Workflow(name="c_repno") public String repNo(String x) { try { Status s = repeatUntil(() -> t.pollNo("k"), Status::done, Backoff.every(Duration.ofSeconds(1)).attempts(2)); return "returned done=" + s.done(); } catch (TaskFailure e) { return "threw"; } }
    @Workflow(name="c_onceloop") public String onceLoop(String x) { String r = ""; for (int i = 0; i < 2; i++) { final int k = i; r += once("v", () -> "n" + k); a(x + i); } return r; }
    @Workflow(name="c_unjoined") public String unjoined(String x) { async(() -> a(x)); return "done"; }
    public record Pair(String l, int r) {}
    @Workflow(name="c_pojowf") public Pair pojoWf(Item i) { return new Pair(a(i.id()), i.tags().size()); }
    @Workflow(name="c_intwf") public int intWf(int n) { return t.num(n) * 2; }
    @Workflow(name="c_listwf") public List<String> listWf(List<String> l) { return fanOut("fan", l, i -> a(i)).get(); }

    @Workflow(name="c_catchthrow") public String catchThrow(String x) { try { return a(x); } catch (Throwable t) { return b(x); } }
    @Workflow(name="c_finallyvar") public String finallyVar(String x) { try { a(x); } finally { count++; } b(x); return "count=" + count; }
    @Workflow(name="c_finallyret") public String finallyLane(String x) { Promise<String> p = async(() -> { try { return a(x); } finally { b(x); } }); join(p); return p.get(); }

    // ---- lanes that run on their own
    @Workflow(name = "c_forgetslow")
    public String forgetSlow(String x) {
        async(() -> t.slow("1500"));
        return a(x);
    }

    @Workflow(name = "c_forgetfails")
    public String forgetFails(String x) {
        async(() -> boom(x));
        return a(x);
    }

    @Workflow(name = "c_forgettwo")
    public String forgetTwoSteps(String x) {
        async(() -> b(a(x)));
        return "done";
    }

    @Workflow(name = "c_waitlater")
    public String waitLater(String x) {
        Promise<String> p = async(() -> t.slow("1500"));
        String r = b(x);
        join(p);
        return c(r);
    }

    @Workflow(name = "c_outoforder")
    public String outOfOrder(String x) {
        Promise<String> p = async(() -> t.slow("2500"));
        Promise<String> q = async(() -> t.slow("800"));
        t.d(x);
        join(q);
        join(p);
        return p.get() + q.get();
    }

    @Workflow(name = "c_joinoneoftwo")
    public String joinOneOfTwo(String x) {
        Promise<String> p = async(() -> a(x));
        async(() -> t.slow("1500"));
        join(p);
        return p.get() + b(x);
    }

    @Workflow(name = "c_nested")
    public String nested(String x) {
        Promise<String> p = async(() -> {
            Promise<String> r = async(() -> a(x));
            join(r);
            return b(r.get());
        });
        join(p);
        return c(p.get());
    }

    @Workflow(name = "c_nestedbubbles")
    public String nestedBubbles(String x) {
        Promise<String> p = async(() -> {
            async(() -> t.slow("1500"));
            return "p";
        });
        join(p);
        return c(x);
    }

    @Workflow(name = "c_asyncincase")
    public String asyncInCase(String x) {
        String r = switchOn("sw", x)
                .when("1", () -> {
                    async(() -> a(x));
                    return b(x);
                })
                .when("2", () -> e(x))
                .get();
        c(r);
        return r;
    }

    @Workflow(name = "c_oncebeforejoin")
    public String onceBeforeJoin(String x) {
        Promise<String> p = async(() -> a(x));
        String v = once("v", () -> "v");
        join(p);
        return v + p.get();
    }

    @Workflow(name = "c_faninlanetwo")
    public String fanInLaneTwo(String x) {
        Promise<String> p = async(() -> fanOut("fan", List.of("p", "q"), i -> a(i)).get().toString());
        Promise<String> q = async(() -> b(x));
        join(p, q);
        return p.get() + q.get();
    }

    @Workflow(name = "c_previewlanes")
    public String previewLanes(String x) {
        switchOn("sw", x)
                .when("later", () -> {
                    Promise<String> p = async(() -> a(x));
                    b(x);
                    join(p);
                    c(x);
                })
                .when("never", () -> {
                    async(() -> e(x));
                    f(x);
                })
                .when("taken", () -> {
                    d(x);
                })
                .get();
        return "done";
    }

    // ---- harness
    interface Hook { void after(WorkflowHandle<String> h) throws Exception; }
    record Case(String id, Functions.Fn1<Conformance,String,String> fn, String input, String want, String tree, Hook hook) {}
    static final List<Case> CASES = new ArrayList<>();
    static void c(String id, Functions.Fn1<Conformance,String,String> fn, String in, String want, String tree) { CASES.add(new Case(id, fn, in, want, tree, null)); }
    static void c(String id, Functions.Fn1<Conformance,String,String> fn, String in, String want, String tree, Hook h) { CASES.add(new Case(id, fn, in, want, tree, h)); }

    static void defs() {
        c("seq", Conformance::seq, "1", "b(a(1))", "ta;tb");
        c("javaloop", Conformance::javaLoop, "1", "a(a(a(1)))", "ta;ta_2;ta_3");
        c("javaif", Conformance::javaIf, "2", "b(2)", "tb");
        c("helper", Conformance::helper, "1", "b(a(b(a(1))))", "ta;tb;ta_2;tb_2");
        c("sw-1", Conformance::sw, "1", "e(b(a(1)))", "ta;sw[1:tb|2:tc_if_2*|default:td_if_default*];te");
        c("sw-2", Conformance::sw, "2", "e(c(a(2)))", "ta;sw[1:tb_if_1*|2:tc|default:td_if_default*];te");
        c("sw-default", Conformance::sw, "9", "e(d(a(9)))", "ta;sw[1:tb_if_1*|2:tc_if_2*|default:td];te");
        c("sw-empty", Conformance::swEmpty, "1", "c(none)", "sw[1:|2:tb_if_2*];tc");
        c("sw-empty-other", Conformance::swEmpty, "2", "c(b(2))", "sw[1:|2:tb];tc");
        c("sw-multi", Conformance::swMulti, "1", "d(c(b(1)))", "sw[1:tb;tc|2:te_if_2*];td");
        c("sw-nocase", Conformance::swNoCase, "9", "FAILED:no case", "sw[1:ta_if_1*];throw");
        c("sw-first", Conformance::swFirst, "1", "c(1)", "sw[1:ta|default:tb_if_default*];tc");
        c("sw-last", Conformance::swLast, "2", "b(2)", "sw[1:ta_if_1*|default:tb]");
        c("sw-seq", Conformance::swSeq, "1", "e(c(a(1)))", "s1[1:ta|default:tb_if_default*];s2[1:tc|default:td_if_default*];te");
        c("sw-nest", Conformance::swNest, "1", "f(d(b(a(1))))", "outer[1:ta;inner[2:tb|3:tc_if_3*];td|2:te_if_2*];tf");
        c("sw-nest-untaken", Conformance::swNest, "2", "f(e(2))", "outer[1:ta_if_1*;inner_if_1*[2:tb_if_1_2*|3:tc_if_1_3*];td_if_1*|2:te];tf");
        c("sw-nest-last", Conformance::swNestLast, "1", "c(a(1))", "outer[1:inner[1:ta|default:tb_if_default*]];tc");
        c("sw-int", Conformance::swInt, "x", "a(x)", "sw[5:ta|default:tb_if_default*]");
        c("sw-bool", Conformance::swBool, "x", "a(x)", "sw[false:tb_if_false*|true:ta]");
        c("sw-null", Conformance::swNull, "x", "a(x)", "sw[null:ta|default:tb_if_default*]");
        c("sw-enum", Conformance::swEnum, "x", "b(x)", "tcolor;sw[BLUE:tb|RED:ta_if_RED*]");
        c("sw-javaloop", Conformance::swJavaLoop, "x", "c(b(a(x)))", "sw[0:ta|1:tb_if_1*];sw_2[0:ta_if_0*|1:tb];tc");
        c("sw-sleep", Conformance::swSleep, "1", "c(a(1))", "sw[1:sleep;ta|default:tb_if_default*];tc");
        c("sw-var", Conformance::swVar, "1", "count=6", "sw[1:ta];tb");
        c("fork", Conformance::fork, "1", "c(a(1)b(1))", "fork{ta|tb};fork_join;tc");
        c("fork-one", Conformance::forkOne, "1", "b(a(1))", "fork{ta};fork_join;tb");
        c("fork-long", Conformance::forkLong, "1", "c(b(a(1)))d(1)", "fork{ta;tb;tc|td};fork_join");
        c("fork-value", Conformance::forkValue, "1", "va(1)", "fork{ta};fork_join");
        c("fork-nojoin", Conformance::forkNoJoin, "1", "a(1)b(1)", "");
        c("fork-main", Conformance::forkMain, "1", "a(1)b(1)", "");
        c("fork-2", Conformance::fork2, "1", "b(a(1))", "fork{ta};fork_join;fork_2{tb};fork_2_join");
        c("fork-javaloop", Conformance::forkJavaLoop, "x", "a(x0)b(x0)a(x1)b(x1)", "fork{ta|tb};fork_join;fork_2{ta_2|tb_2};fork_2_join");
        c("fork-sw", Conformance::forkSw, "1", "e(c(a(1))d(1))", "fork{sw[1:ta|default:tb_if_default*];tc|td};fork_join;te");
        c("fork-sw-2", Conformance::forkSw2, "1", "a(1)c(1)", "fork{s1[1:ta|default:tb_if_default*]|s2[1:tc|default:td_if_default*]};fork_join");
        c("fork-in-case", Conformance::forkInCase, "1", "d(c(a(1)b(1)))", "sw[1:fork{ta|tb};fork_join;tc|2:te_if_2*];td");
        c("fork-in-case-untaken", Conformance::forkInCase, "2", "d(e(2))", "sw[1:fork_if_1*{ta_if_1*|tb_if_1*};fork_if_1_join*;tc_if_1*|2:te];td");
        c("fork-last-in-case", Conformance::forkLastInCase, "1", "d(1)", "sw[1:fork{ta};fork_join];td");
        c("fork-async", Conformance::forkAsync, "1", "a(1)", "");
        c("fork-sleep", Conformance::forkSleep, "1", "a(1)b(1)", "fork{sleep;ta|tb};fork_join");
        c("fan-0", Conformance::fan, "0", "b([])", "tlist;tb");
        c("fan-1", Conformance::fan, "1", "b([a(i0)])", "tlist;fan{ta};fan_join;tb");
        c("fan-3", Conformance::fan, "3", "b([a(i0), a(i1), a(i2)])", "tlist;fan{ta|ta_2|ta_3};fan_join;tb");
        c("fan-multi", Conformance::fanMulti, "x", "[b(a(i0)), b(a(i1))]", "");
        c("fan-in-case", Conformance::fanInCase, "1", "b([a(i0), a(i1)])", "sw[1:tlist;fan{ta|ta_2};fan_join|2:te_if_2*];tb");
        c("fan-in-case-untaken", Conformance::fanInCase, "2", "b(e(2))", "sw[1:tlist_if_1*;fan_if_1~*;fan_if_1_join*|2:te];tb");
        c("fan-lane", Conformance::fanLane, "x", "[a(q)]", "");
        c("fan-twice", Conformance::fanTwice, "x", "[a(p)][b(q)]", "fanout{ta};fanout_join;fanout_2{tb};fanout_2_join");
        c("fan-javaloop", Conformance::fanJavaLoop, "x", "[a(i0)][a(i1)]", "fan{ta};fan_join;fan_2{ta_2};fan_2_join");
        c("fan-sw", Conformance::fanSw, "x", "[a(1), b(2)]", "fan{sw[1:ta|default:tb_if_default*]|sw_2[1:ta_if_1*|default:tb]};fan_join");
        c("each", Conformance::each, "x", "b([a(p), a(q), a(r)])", "loop(loop_task);tb");
        c("each-empty", Conformance::eachEmpty, "x", "b([])", "tb");
        c("each-case", Conformance::eachCase, "1", "b([a(p)])", "sw[1:loop(loop_task)|2:te_if_2*];tb");
        c("each-lane", Conformance::eachLane, "x", "[a(p), a(q)]b(x)", "fork{loop(loop_task)|tb};fork_join");
        c("each-javaloop", Conformance::eachJavaLoop, "x", "[a(i0)][a(i1)]", "loop(loop_task);loop_2(loop_2_task)");
        c("rep", Conformance::rep, "x", "a(n=3)", "poll(poll_backoff;poll_wait;poll_check);ta");
        c("rep-case", Conformance::repCase, "1", "a(n=3)", "sw[1:poll(poll_backoff;poll_wait;poll_check)|2:te_if_2*];ta");
        c("rep-lane", Conformance::repLane, "x", "n=3b(x)", "fork{poll(poll_backoff;poll_wait;poll_check)|tb};fork_join");
        c("rep-after", Conformance::repAfter, "x", "a(3,3)", "poll(poll_backoff;poll_wait;poll_check);poll_2(poll_2_backoff;poll_2_wait;poll_2_check);ta");
        c("fail-caught", Conformance::failCaught, "1", "b(1)", "tboom;tb");
        c("fail-uncaught", Conformance::failUncaught, "1", "FAILED:boom", "ta;tboom");
        c("fail-java", Conformance::failJava, "1", "FAILED:plain 1", "ta;throw");
        c("fail-case", Conformance::failCase, "1", "c(caught)", "sw[1:tboom|default:ta_if_default*];tc");
        c("fail-lane-caught", Conformance::failLaneCaught, "1", "a(1)b(1)", "fork{tboom;ta|tb};fork_join");
        c("fail-lane", Conformance::failLane, "1", "FAILED:boom", "fork{tboom|tb};fork_join");
        c("fail-join-caught", Conformance::failJoinCaught, "1", "c(caught:b(1))", "fork{tboom|tb};fork_join;tc");
        c("fail-compensate", Conformance::failCompensate, "1", "c(a(1))", "ta;tboom;tboom_2;tc");
        c("child", Conformance::childCall, "1", "b(a(kid:1))", "c_kid;tb");
        c("child-case", Conformance::childCase, "1", "b(a(kid:1))", "sw[1:c_kid|2:te_if_2*];tb");
        c("child-lane", Conformance::childLane, "x", "a(kid:l)a(kid:r)", "fork{c_kid|c_kid_2};fork_join");
        c("child-fail", Conformance::childFail, "1", "b(1)", "c_kidfail;tb");
        c("once", Conformance::onceStable, "x", "stable", "ta;tb");
        c("nowrandom", Conformance::nowRandom, "x", "stable", "ta");
        c("sleep", Conformance::sleepIt, "x", "a(x)", "sleep;ta");
        c("var", Conformance::var, "x", "count=111 log=[one, two]", "ta;tb");
        c("var-lane", Conformance::varLane, "x", "[L, R]", "fork{ta|tb};fork_join");
        c("signal", Conformance::signal, "x", "b(true[sig:hi])", "ta;condition;tb", h -> { Thread.sleep(1500); h.signal(Conformance::go, "hi"); });
        c("cond-timeout", Conformance::condTimeout, "x", "a(false)", "condition;ta");
        c("wfid", Conformance::wfId, "x", "same", "ta");
        c("void", Conformance::voidTask, "x", "a(x)", "tvoid;ta");
        c("null", Conformance::nullTask, "x", "a(was-null)", "tnull;ta");
        c("pojo", Conformance::pojo, "x", "a(x[t1, t2])", "titem;ta");
        c("pojo-list", Conformance::pojoList, "x", "a(x2[q]Item)", "titems;ta");
        c("map", Conformance::map, "x", "a(8)", "tmap;ta");
        c("int", Conformance::num, "x", "a(42)", "tint;ta");
        c("named", Conformance::named, "x", "b(a(x))", "zed;tb");
        c("ref-dup", Conformance::refDup, "x", "b(x)", "zed;zed_2");
        c("ref-loop", Conformance::refLoop, "x", "a(a(a(x)))", "zed;zed_2;zed_3");
        c("ref-unrolled", Conformance::refUnrolled, "x", "a(a(a(x)))", "zed;zed_2;zed_3");
        c("instant", Conformance::instant, "x", "a(2026-01-02T03:04:05Z)", "tinstant;ta");
        c("uuid", Conformance::uuid, "x", "a(00000000-0000-0000-0000-000000000007)", "tuuid;ta");
        c("array", Conformance::arr, "x", "a(xy)", "tarr;ta");
        c("bool", Conformance::bool, "x", "a(true)", "tbool;ta");
        c("enum-in", Conformance::colorInput, "x", "got:RED", "tcolorin");
        c("pojo-in", Conformance::itemInput, "x", "z[k]", "titemin");
        c("pojolist-in", Conformance::itemsInput, "x", "a(2)", "titemsin;ta");
        c("two-args", Conformance::twoArgs, "x", "p5", "ttwo");
        c("dollar-input", Conformance::d1, "x", "a(${workflow.input.x})", "ta");
        c("dollar-odd", Conformance::d2, "x", "a($${x} $ {y} ${)", "ta");
        c("dollar-output", Conformance::d3, "x", "a(${workflow.input})", "tdollar;ta");
        c("dollar-switch", Conformance::d4, "x", "a(x)", "sw[${workflow.input}:ta|default:tb_if_default*]");
        c("dollar-child", Conformance::d5, "x", "a(kid:${workflow.input})", "c_kid");
        c("dollar-each", Conformance::d6, "x", "[a(${a}), a($${b})]", "loop(loop_task)");
        c("dollar-fan", Conformance::d7, "x", "[a(${a})]", "fan{ta};fan_join");
        c("dollar-once", Conformance::d8, "x", "b(${workflow.input})", "ta;tb");
        c("dollar-var", Conformance::d9, "x", "b(${workflow.input})", "ta;tb");
        c("dollar-named", Conformance::d10, "x", "a(${workflow.input})", "ta");
        c("dollar-wfinput", Conformance::d11, "${workflow.input}", "${workflow.input}", "");
        c("dollar-pojo", Conformance::d12, "x", "${a}[${b}]", "titemin");
        c("unicode", Conformance::uni, "x", "a(\u00e9\"\n'\\\t\u4e16)", "ta");
        c("null-input", Conformance::nullIn, "x", "a(null)", "ta");
        c("key-special", Conformance::key, "x", "a(x)", "sw[:tc_if_*|a-b c.d:ta|x y:tb_if_x_y*]");
        c("key-empty", Conformance::keyEmpty, "x", "a(x)", "sw[:ta|default:tb_if_default*]");
        c("fan-order", Conformance::fanOrder, "x", "[s800, s1, s300]", "fan{tslow|tslow_2|tslow_3};fan_join");
        c("fan-large", Conformance::fanLarge, "x", "true:40", "");
        c("flaky", Conformance::flakyWf, "x", "a(ok after 3)", "tflaky;ta");
        c("var-untaken", Conformance::varUntaken, "2", "count=100", "sw[1:ta_if_1*];tb");
        c("local-list", Conformance::localList, "a", "[a(a)]", "sw[a:ta|b:tb_if_b*;tc_if_b*]");
        c("preview-all", Conformance::previewAll, "z", "d(z)", "");
        c("await-lane", Conformance::awaitLane, "x", "a(true)b(x)[sig:hi]", "", h -> { Thread.sleep(1500); h.signal(Conformance::go, "hi"); });
        c("await-case", Conformance::awaitCase, "1", "b(a(true))", "", h -> { Thread.sleep(1500); h.signal(Conformance::go, "hi"); });
        c("two-signals", Conformance::twoSig, "x", "[sig:1, sig:2]", "", h -> { h.signal(Conformance::go, "1"); h.signal(Conformance::go, "2"); });
        c("ext-case", Conformance::extCase, "1", "a(posted)", "", h -> h.completeTask("cb", "posted", Duration.ofSeconds(30)));
        c("ext-lane", Conformance::extLane, "x", "postedb(x)", "", h -> h.completeTask("cb", "posted", Duration.ofSeconds(30)));
        c("empty", Conformance::emptyWf, "x", "vx", "");
        c("empty-fail", Conformance::emptyFail, "x", "FAILED:nope", "");
        c("empty-null", Conformance::emptyNull, "x", "null", "");
        // each level's call is in the false arm of its base case (the code after a guard is its false arm)
        c("recursion", Conformance::recur, "x", "4",
                "if_n[true:|default:ta;if_n_2[true:|default:ta_2;if_n_3[true:|default:ta_3;if_n_4[true:|default:ta_4]]]]");
        c("long", Conformance::longSeq, "x", "a(59)", "");
        c("finally", Conformance::tryFinally, "x", "cb(x)", "tboom;tb");
        c("break", Conformance::breakLoop, "x", "i=2", "ta;ta_2;ta_3");
        c("fail-info", Conformance::failInfo, "x", "tboom|FAILED_WITH_TERMINAL_ERROR", "tboom");
        c("grandchild", Conformance::grand, "x", "b(a(kid:x))", "c_mid");
        c("start-wf", Conformance::startWf, "x", "a(x)", "");
        c("sleep-zero", Conformance::sleepZero, "x", "a(x)", "");
        c("sleep-past", Conformance::sleepPast, "x", "a(x)", "");
        c("rep-exhausted", Conformance::repNo, "x", "returned done=false", "");
        c("once-loop", Conformance::onceLoop, "x", "n0n1", "ta;ta_2");
        c("catch-throwable", Conformance::catchThrow, "x", "a(x)", "ta");
        c("finally-var", Conformance::finallyVar, "x", "count=1", "ta;tb");
        c("finally-lane", Conformance::finallyLane, "x", "a(x)", "fork{ta;tb};fork_join");
        c(
                "preview-lanes",
                Conformance::previewLanes,
                "taken",
                "done",
                "sw[later:fork_if_later*{ta_if_later*};fork_if_later_join*;tb_if_later*;fork_wait_if_later*;tc_if_later*|never:fork_if_never*{te_if_never*};fork_if_never_join*;tf_if_never*|taken:td]");
        c("unjoined", Conformance::unjoined, "x", "done", "fork{ta};fork_join");
        c("forget-slow", Conformance::forgetSlow, "x", "a(x)", "fork{tslow};fork_join;ta");
        c("forget-fails", Conformance::forgetFails, "x", "a(x)", "fork{tboom};fork_join;ta");
        c("forget-two-steps", Conformance::forgetTwoSteps, "x", "done", "fork{ta;tb};fork_join");
        c("wait-later", Conformance::waitLater, "x", "c(b(x))", "fork{tslow};fork_join;tb;fork_wait;tc");
        c("wait-out-of-order", Conformance::outOfOrder, "x", "s2500s800", "");
        c("join-one-of-two", Conformance::joinOneOfTwo, "x", "a(x)b(x)", "fork{ta|tslow};fork_join;tb");
        c("nested", Conformance::nested, "x", "c(b(a(x)))", "fork_2{fork{ta};fork_join;tb};fork_2_join;tc");
        c("nested-bubbles", Conformance::nestedBubbles, "x", "c(x)", "fork{tslow};fork_join;fork_wait;tc");
        c("async-in-case", Conformance::asyncInCase, "1", "b(1)", "sw[1:fork{ta};fork_join;tb|2:te_if_2*];tc");
        c("once-before-join", Conformance::onceBeforeJoin, "x", "va(x)", "fork{ta};fork_join");
        c("fan-in-lane-two", Conformance::fanInLaneTwo, "x", "[a(p), a(q)]b(x)", "");
    }

    // ---- checks
    static final ObjectMapper M = new ObjectMapper();
    @SuppressWarnings("unchecked") static String render(List<Map<String,Object>> l) { List<String> s = new ArrayList<>(); for (var e : l) s.add(render(e)); return String.join(";", s); }
    @SuppressWarnings("unchecked") static String render(Map<String,Object> e) {
        String ref = (String) e.get("taskReferenceName"), type = (String) e.get("type");
        Map<String,Object> md = (Map<String,Object>) e.getOrDefault("metadata", Map.of());
        String star = Boolean.TRUE.equals(md.get("preview")) ? "*" : "";
        switch (type) {
            case "SWITCH": { List<String> p = new ArrayList<>(); var cases = new TreeMap<>((Map<String,List<Map<String,Object>>>) e.get("decisionCases")); cases.forEach((k, v) -> p.add(k + ":" + render(v))); var d = (List<Map<String,Object>>) e.get("defaultCase"); if (d != null && !d.isEmpty()) p.add("default:" + render(d)); return ref + star + "[" + String.join("|", p) + "]"; }
            case "FORK_JOIN": { List<String> p = new ArrayList<>(); for (var b : (List<List<Map<String,Object>>>) e.get("forkTasks")) p.add(render(b)); return ref + star + "{" + String.join("|", p) + "}"; }
            case "FORK_JOIN_DYNAMIC": return ref + "~" + star;
            case "DO_WHILE": { return ref + star + "(" + render((List<Map<String,Object>>) e.get("loopOver")) + ")"; }
            default: return ref + star;
        }
    }
    @SuppressWarnings("unchecked") static void invariants(List<Map<String,Object>> list, Map<String,Map<String,Object>> byRef, Map<String,Boolean> preview, List<String> bad, boolean live) {
        int last = -1; String lastRef = null;
        for (var e : list) {
            String ref = (String) e.get("taskReferenceName"); String type = (String) e.get("type");
            boolean pv = Boolean.TRUE.equals(((Map<String,Object>) e.getOrDefault("metadata", Map.of())).get("preview"));
            preview.put(ref, pv);
            Map<String,Object> t = byRef.get(ref);
            if (t != null && !live) bad.add("task " + ref + " ran inside an untaken case");
            if (t != null && pv) bad.add("preview " + ref + " was scheduled");
            if (t != null) { int seq = ((Number) t.get("seq")).intValue(); if (live && seq < last) bad.add("order: " + ref + " before " + lastRef); last = seq; lastRef = ref; }
            if ("SWITCH".equals(type)) {
                Object sel = t == null ? null : ((Map<String,Object>) t.get("outputData")).get("selectedCase");
                var cases = (Map<String,List<Map<String,Object>>>) e.get("decisionCases");
                boolean matched = sel != null && cases.containsKey(String.valueOf(sel));
                cases.forEach((k, v) -> invariants(v, byRef, preview, bad, live && t != null && k.equals(String.valueOf(sel))));
                var d = (List<Map<String,Object>>) e.get("defaultCase");
                if (d != null) invariants(d, byRef, preview, bad, live && t != null && !matched);
            }
            if ("FORK_JOIN".equals(type)) for (var b : (List<List<Map<String,Object>>>) e.get("forkTasks")) invariants(b, byRef, preview, bad, live && t != null);
        }
    }

    @SuppressWarnings("unchecked") static Map<String,Object> get(Conductor c, String id) {
        return c.client().execute(ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + id).addQueryParam("includeTasks", true).build(), new TypeReference<Map<String,Object>>() {}).getData();
    }

    @SuppressWarnings("unchecked") static String runCase(Conductor c, Case k, Path dir) {
        List<String> bad = new ArrayList<>();
        WorkflowHandle<String> h;
        try { h = c.start(k.fn, k.input); } catch (Throwable t) { return "FAIL start: " + t; }
        try {
            if (k.hook != null) k.hook.after(h);
            String got; try { got = h.result(Duration.ofSeconds(60)); } catch (Throwable t) { got = "THROW:" + t; }
            Map<String,Object> wf = get(c, h.workflowId());
            String status = (String) wf.get("status");
            if ("RUNNING".equals(status)) return "FAIL still RUNNING " + h.workflowId() + " " + got;
            if (k.want.startsWith("FAILED:")) {
                String why = String.valueOf(wf.get("reasonForIncompletion"));
                if (!"FAILED".equals(status)) bad.add("want FAILED got " + status + " output " + got);
                else if (!why.contains(k.want.substring(7))) bad.add("reason: " + why);
            } else {
                if (!"COMPLETED".equals(status)) bad.add("status " + status + " reason " + wf.get("reasonForIncompletion"));
                else if (!k.want.equals(String.valueOf(got))) bad.add("result want " + k.want + " got " + got);
            }
            if ("COMPLETED".equals(status)) {
                for (var t : (List<Map<String, Object>>) wf.get("tasks")) {
                    if (Set.of("IN_PROGRESS", "SCHEDULED").contains(t.get("status"))) {
                        bad.add("completed with " + t.get("referenceTaskName") + " still " + t.get("status"));
                    }
                }
            }
            var def = (Map<String,Object>) wf.get("workflowDefinition");
            var defTasks = (List<Map<String,Object>>) def.get("tasks");
            String tree = render(defTasks);
            if (!k.tree.isEmpty() && !k.tree.equals(tree)) bad.add("tree\n      want " + k.tree + "\n      got  " + tree);
            Map<String,Map<String,Object>> byRef = new HashMap<>();
            for (var t : (List<Map<String,Object>>) wf.get("tasks")) { byRef.put((String) t.get("referenceTaskName"), t); }
            Map<String,Boolean> preview = new HashMap<>();
            invariants(defTasks, byRef, preview, bad, true);
            for (String ref : byRef.keySet()) { String base = ref.replaceAll("__\\d+$", ""); if (!preview.containsKey(base) && !allRefs(defTasks).contains(base)) bad.add("task " + ref + " is not in the definition"); }
            System.out.println("TREE " + k.id + " " + status + " " + tree);
            Path p = dir.resolve(k.id + ".json"); Files.write(p, M.writeValueAsBytes(wf));
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) { try { DurableReplay.assertCompatible(Conformance.class, p); } catch (Throwable t) { bad.add("replay: " + t.getMessage()); } }
            return bad.isEmpty() ? "PASS" : "FAIL " + h.workflowId() + " " + String.join(" ; ", bad);
        } catch (Throwable t) { return "FAIL harness " + h.workflowId() + ": " + t; }
    }
    @SuppressWarnings("unchecked") static Set<String> allRefs(List<Map<String,Object>> l) { Set<String> s = new HashSet<>(); if (l == null) return s; for (var e : l) { s.add((String) e.get("taskReferenceName")); s.addAll(allRefs((List<Map<String,Object>>) e.get("defaultCase"))); s.addAll(allRefs((List<Map<String,Object>>) e.get("loopOver"))); var dc = (Map<String,List<Map<String,Object>>>) e.get("decisionCases"); if (dc != null) dc.values().forEach(v -> s.addAll(allRefs(v))); var f = (List<List<Map<String,Object>>>) e.get("forkTasks"); if (f != null) f.forEach(v -> s.addAll(allRefs(v))); } return s; }

    public static void main(String[] args) throws Exception {
        if (System.getenv("CONDUCTOR_SERVER_URL") == null) {
            throw new IllegalStateException("set CONDUCTOR_SERVER_URL to the server to test");
        }
        defs();
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor c = Conductor.of(client);
        c.host().workflows(Conformance.class).tasks(new T()).start();
        Path dir = Files.createTempDirectory("conformance");
        String only = args.length > 0 ? args[0] : null;
        ExecutorService pool = Executors.newFixedThreadPool(6);
        Map<String,Future<String>> res = new LinkedHashMap<>();
        for (Case k : CASES) if (only == null || k.id.matches(only)) res.put(k.id, pool.submit(() -> runCase(c, k, dir)));
        int pass = 0, fail = 0;
        for (var e : res.entrySet()) { String r = e.getValue().get(); if (r.equals("PASS")) { pass++; System.out.println("ok   " + e.getKey()); } else { fail++; System.out.println("BAD  " + e.getKey() + ": " + r); } }
        if (only == null) {
            var d = Duration.ofSeconds(60);
            fail += typed("pojo workflow", new Pair("a(i)", 2), c.start(Conformance::pojoWf, new Item("i", List.of("a", "b"))).result(d));
            fail += typed("int workflow", 42, c.start(Conformance::intWf, 20).result(d));
            fail += typed("list workflow", List.of("a(p)", "a(q)"), c.start(Conformance::listWf, List.of("p", "q")).result(d));
        }
        System.out.println("CONFORMANCE pass=" + pass + " fail=" + fail);
        pool.shutdown();
        c.close();
        client.shutdown();
        System.exit(fail == 0 ? 0 : 1);
    }

    private static int typed(String what, Object want, Object got) {
        boolean ok = want.equals(got);
        System.out.println((ok ? "ok   " : "BAD  ") + what + (ok ? "" : ": want " + want + " got " + got));
        return ok ? 0 : 1;
    }
}
