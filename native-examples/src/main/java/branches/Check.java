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
package branches;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;
import org.conductoross.conductor.sdk.testing.DurableReplay;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Runs every workflow of {@link Branches} on a server and checks each execution: its result, its
 * drawing, that no preview ran, and that it replays against the code.
 *
 * <p>The drawing is written with the markers in Branches.java: {@code #i2:false{b};e} is the if
 * marked {@code #i2}, drawn with its false case holding task {@code b}, then task {@code e} after it.
 * {@code ;} is sequence, {@code throw} the mark where the code threw. Expectations follow the rule
 * that a call is inside an arm when it runs only because that arm was taken.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" branches.Check [id regex]
 * </pre>
 */
public class Check {

    record Case(String id, Function<Conductor, WorkflowHandle<String>> start, String want, String tree) {}

    static final List<Case> CASES = new ArrayList<>();

    static void c(String id, Function<Conductor, WorkflowHandle<String>> start, String want, String tree) {
        CASES.add(new Case(id, start, want, tree));
    }

    static void defs() {
        // if
        c("if-only-taken", k -> k.start(Branches::ifOnly, "1"), "d(a(1))", "#i1:true{a};d");
        c("if-only-passed", k -> k.start(Branches::ifOnly, "2"), "d(2)", "#i1:false{};d");
        c("if-else-then", k -> k.start(Branches::ifElse, "1"), "d(a(1))", "#i2:true{a};d");
        c("if-else-else", k -> k.start(Branches::ifElse, "2"), "d(b(2))", "#i2:false{b};d");
        c("else-if-1", k -> k.start(Branches::elseIf, "1"), "e(a(1))", "#i3:true{a};e");
        c("else-if-2", k -> k.start(Branches::elseIf, "2"), "e(b(2))", "#i3:false{#i4:true{b}};e");
        c("else-if-3", k -> k.start(Branches::elseIf, "3"), "e(c(3))", "#i3:false{#i4:false{#i5:true{c}}};e");
        c("else-if-else", k -> k.start(Branches::elseIf, "4"), "e(d(4))", "#i3:false{#i4:false{#i5:false{d}}};e");
        c("else-if-no-else-2", k -> k.start(Branches::elseIfNoElse, "2"), "e(b(2))", "#i6:false{#i7:true{b}};e");
        c("else-if-no-else-none", k -> k.start(Branches::elseIfNoElse, "3"), "e(3)", "#i6:false{#i7:false{}};e");
        c("nested-if-3", k -> k.start(Branches::nestedIf, "3"), "e(a(3))", "#i8:true{#i9:true{a}};e");
        c("nested-if-2", k -> k.start(Branches::nestedIf, "2"), "e(b(2))", "#i8:true{#i9:false{b}};e");
        c("nested-if-1", k -> k.start(Branches::nestedIf, "1"), "e(c(1))", "#i8:false{c};e");
        c("no-braces-then", k -> k.start(Branches::noBraces, "1"), "e(a(1))", "#i10:true{a};e");
        c("no-braces-else", k -> k.start(Branches::noBraces, "2"), "e(b(2))", "#i10:false{b};e");
        c("task-condition-true", k -> k.start(Branches::taskCondition, "y1"), "e(a(y1))", "flag;#i11:true{a};e");
        c("task-condition-false", k -> k.start(Branches::taskCondition, "n1"), "e(b(n1))", "flag;#i11:false{b};e");
        c("and-or-and", k -> k.start(Branches::andOr, "1"), "e(a(1))", "#i12:true{a};e");
        c("and-or-or", k -> k.start(Branches::andOr, "9"), "e(a(9))", "#i12:true{a};e");
        c("and-or-false", k -> k.start(Branches::andOr, "5"), "e(b(5))", "#i12:false{b};e");
        c("negated-true", k -> k.start(Branches::negated, "1"), "e(a(1))", "#i13:true{a};e");
        c("negated-false", k -> k.start(Branches::negated, "5"), "e(b(5))", "#i13:false{b};e");
        c("empty-then", k -> k.start(Branches::emptyThen, "1"), "e(1)", "#i14:true{};e");
        c("empty-then-else", k -> k.start(Branches::emptyThen, "2"), "e(b(2))", "#i14:false{b};e");
        c("guard-return-taken", k -> k.start(Branches::guardReturn, "1"), "a(1)", "#i15:true{a}");
        c("guard-return-passed", k -> k.start(Branches::guardReturn, "2"), "c(b(2))", "#i15:false{b;c}");
        c("guard-continue", k -> k.start(Branches::guardContinue, List.of("o-1", "bad-2", "o-3")), "shipped 2",
                "#i16:false{charge;ship};#i16:true{reject};#i16:false{charge;ship}");
        c("guard-break", k -> k.start(Branches::guardBreak, List.of("o-1", "stop", "o-3")), "done 1",
                "#i17:false{charge};#i17:true{reject}");
        c("guard-throw-ok", k -> k.start(Branches::guardThrow, "ok"), "a(ok)", "#i18:false{#i19:false{a}}");
        c("guard-throw-bad", k -> k.start(Branches::guardThrow, "bad"), "FAILED:rejected bad", "#i18:false{#i19:true{reject;throw}}");
        c("guard-throw-empty", k -> k.start(Branches::guardThrow, ""), "FAILED:empty input", "#i18:true{throw}");
        c("ternary-one-line", k -> k.start(Branches::ternaryOneLine, "1"), "e(a(1))", "a;e");
        c("ternary-lines-then", k -> k.start(Branches::ternaryLines, "1"), "e(a(1))", "#i21:true{a};e");
        c("ternary-lines-else", k -> k.start(Branches::ternaryLines, "2"), "e(b(2))", "#i21:false{b};e");
        c("instanceof-big", k -> k.start(Branches::instanceOf, "9"), "e(a(9))", "value;#i22:true{a};e");
        c("instanceof-small", k -> k.start(Branches::instanceOf, "3"), "e(b(3))", "value;#i22:false{b};e");
        c("instanceof-text", k -> k.start(Branches::instanceOf, "z"), "e(b(z))", "value;#i22:false{b};e");
        c("if-in-loop", k -> k.start(Branches::ifInLoop, "x"), "a(b(a(x)))", "#i23:true{a};#i23:false{b};#i23:true{a}");
        c("return-both-then", k -> k.start(Branches::returnBoth, "1"), "a(d(1))", "d;#i24:true{a}");
        c("return-both-else", k -> k.start(Branches::returnBoth, "2"), "b(d(2))", "d;#i24:false{b}");
        // switch statements
        c("switch-old-1", k -> k.start(Branches::switchOld, "1"), "e(a(1))", "#s1:1{a};e");
        c("switch-old-2", k -> k.start(Branches::switchOld, "2"), "e(b(2))", "#s1:2{b};e");
        c("switch-old-default", k -> k.start(Branches::switchOld, "7"), "e(c(7))", "#s1:default{c};e");
        c("switch-stacked", k -> k.start(Branches::switchStacked, "2"), "e(a(2))", "#s2:1,2{a};e");
        c("switch-stacked-default", k -> k.start(Branches::switchStacked, "3"), "e(b(3))", "#s2:default{b};e");
        c("switch-fallthrough-1", k -> k.start(Branches::switchFallthrough, "1"), "e(b(a(1)))", "a;b;e");
        c("switch-fallthrough-2", k -> k.start(Branches::switchFallthrough, "2"), "e(b(2))", "b;e");
        c("switch-arrow-1", k -> k.start(Branches::switchArrow, "1"), "e(a(1))", "#s4:1{a};e");
        c("switch-arrow-block", k -> k.start(Branches::switchArrow, "2"), "e(c(b(2)))", "#s4:2{b;c};e");
        c("switch-arrow-default", k -> k.start(Branches::switchArrow, "3"), "e(d(3))", "#s4:default{d};e");
        c("switch-labels-2", k -> k.start(Branches::switchLabels, "2"), "e(a(2))", "#s5:1,2{a};e");
        c("switch-labels-4", k -> k.start(Branches::switchLabels, "4"), "e(b(4))", "#s5:3,4,5{b};e");
        c("switch-labels-default", k -> k.start(Branches::switchLabels, "9"), "e(c(9))", "#s5:default{c};e");
        c("switch-default-first-1", k -> k.start(Branches::switchDefaultFirst, "1"), "e(a(1))", "#s6:1{a};e");
        c("switch-default-first-default", k -> k.start(Branches::switchDefaultFirst, "7"), "e(c(7))", "#s6:default{c};e");
        c("switch-no-default-1", k -> k.start(Branches::switchNoDefault, "1"), "e(a(1))", "#s7:1{a};e");
        c("switch-no-default-none", k -> k.start(Branches::switchNoDefault, "3"), "e(3)", "#s7:default{};e");
        c("switch-empty-case", k -> k.start(Branches::switchEmptyCase, "1"), "e(1)", "#s8:1{};e");
        c("switch-empty-case-2", k -> k.start(Branches::switchEmptyCase, "2"), "e(b(2))", "#s8:2{b};e");
        c("switch-string-Aa", k -> k.start(Branches::switchString, "Aa"), "e(a(Aa))", "#s9:Aa{a};e");
        c("switch-string-BB", k -> k.start(Branches::switchString, "BB"), "e(b(BB))", "#s9:BB{b};e");
        c("switch-string-labels", k -> k.start(Branches::switchString, "green"), "e(c(green))", "#s9:red,green{c};e");
        c("switch-string-default", k -> k.start(Branches::switchString, "zz"), "e(d(zz))", "#s9:default{d};e");
        c("switch-enum-red", k -> k.start(Branches::switchEnum, "1"), "e(a(1))", "color;#s10:RED{a};e");
        c("switch-enum-blue", k -> k.start(Branches::switchEnum, "3"), "e(c(3))", "color;#s10:BLUE{c};e");
        c("switch-enum-qualified", k -> k.start(Branches::switchEnumQualified, "1"), "e(a(1))", "color;#s11:RED{a};e");
        c("switch-enum-qualified-labels", k -> k.start(Branches::switchEnumQualified, "3"), "e(b(3))", "color;#s11:GREEN,BLUE{b};e");
        c("switch-char-a", k -> k.start(Branches::switchChar, "a1"), "e(a(a1))", "#s12:a{a};e");
        c("switch-char-default", k -> k.start(Branches::switchChar, "z"), "e(c(z))", "#s12:default{c};e");
        c("switch-boxed", k -> k.start(Branches::switchBoxed, "2"), "e(b(2))", "#s13:2{b};e");
        c("switch-task-value", k -> k.start(Branches::switchTaskValue, "2"), "e(b(2))", "pick;#s14:right{b};e");
        c("switch-task-value-default", k -> k.start(Branches::switchTaskValue, "5"), "e(c(5))", "pick;#s14:default{c};e");
        c("switch-return-1", k -> k.start(Branches::switchReturn, "1"), "a(1)", "#s15:1{a}");
        c("switch-return-none", k -> k.start(Branches::switchReturn, "3"), "c(3)", "#s15:default{c}");
        c("switch-in-loop", k -> k.start(Branches::switchInLoop, List.of("vip-1", "bad-2", "std-3")), "shipped 2",
                "#s16:vip{charge;ship};#s16:bad{reject};#s16:default{ship}");
        c("switch-nested-11", k -> k.start(Branches::switchNested, "11"), "e(a(11))", "#s17:1{#s18:1{a}};e");
        c("switch-nested-12", k -> k.start(Branches::switchNested, "12"), "e(b(12))", "#s17:1{#s18:default{b}};e");
        c("switch-nested-25", k -> k.start(Branches::switchNested, "25"), "e(c(25))", "#s17:default{c};e");
        // switch expressions
        c("switch-expression-1", k -> k.start(Branches::switchExpression, "1"), "e(a(1))", "#x1:1{a};e");
        c("switch-expression-default", k -> k.start(Branches::switchExpression, "5"), "e(c(5))", "#x1:default{c};e");
        c("switch-yield-1", k -> k.start(Branches::switchYield, "1"), "e(a(1))", "#x2:1{a};e");
        c("switch-yield-block", k -> k.start(Branches::switchYield, "2"), "e(c(b(2)))", "#x2:default{b;c};e");
        c("switch-expression-block", k -> k.start(Branches::switchExpressionBlock, "2"), "e(c(b(2)))", "#x3:2{b;c};e");
        c("switch-expression-no-call", k -> k.start(Branches::switchExpressionBlock, "z"), "e(z)", "#x3:default{};e");
        c("switch-expression-enum", k -> k.start(Branches::switchExpressionEnum, "2"), "e(b(2))", "color;#x4:GREEN{b};e");
        // pattern matching
        c("switch-sealed-circle", k -> k.start(Branches::switchSealed, "circle"), "e(a(circle 1))", "#p1:Circle{a};e");
        c("switch-sealed-triangle", k -> k.start(Branches::switchSealed, "tri"), "e(c(triangle 3))", "#p1:Triangle{c};e");
        c("switch-guarded-big", k -> k.start(Branches::switchGuarded, "9"), "e(a(big 9))", "value;#p2:Integer when{a};e");
        c("switch-guarded-small", k -> k.start(Branches::switchGuarded, "3"), "e(b(small 3))", "value;#p2:Integer{b};e");
        c("switch-guarded-text", k -> k.start(Branches::switchGuarded, "z"), "e(c(text z))", "value;#p2:String{c};e");
        c("switch-null", k -> k.start(Branches::switchNull, "none"), "e(a(null))", "#p3:null{a};e");
        c("switch-null-value", k -> k.start(Branches::switchNull, "1"), "e(b(1))", "#p3:1{b};e");
        c("switch-null-default", k -> k.start(Branches::switchNullDefault, "none"), "e(b(null))", "#p4:default{b};e");
        // nested mixes
        c("mix-if-in-case-even", k -> k.start(Branches::mixIfInCase, "12"), "e(a(12))", "#n1:1{#n2:true{a}};e");
        c("mix-if-in-case-odd", k -> k.start(Branches::mixIfInCase, "11"), "e(b(11))", "#n1:1{#n2:false{b}};e");
        c("mix-if-in-case-2", k -> k.start(Branches::mixIfInCase, "25"), "e(c(25))", "#n1:2{c};e");
        c("mix-if-in-case-default", k -> k.start(Branches::mixIfInCase, "35"), "e(d(35))", "#n1:default{d};e");
        c("mix-switch-in-if-v2", k -> k.start(Branches::mixSwitchInIf, "v2"), "e(b(v2))", "#n3:true{#n4:v2{b}};e");
        c("mix-switch-in-if-v9", k -> k.start(Branches::mixSwitchInIf, "v9"), "e(c(v9))", "#n3:true{#n4:default{c}};e");
        c("mix-switch-in-if-1", k -> k.start(Branches::mixSwitchInIf, "1"), "e(d(1))", "#n3:false{#n5:1{d}};e");
        c("mix-switch-in-if-9", k -> k.start(Branches::mixSwitchInIf, "9"), "e(9)", "#n3:false{#n5:default{}};e");
        c("mix-else-if-switches-5", k -> k.start(Branches::mixElseIfSwitches, "5"), "e(a(5))", "#n6:true{a};e");
        c("mix-else-if-switches-31", k -> k.start(Branches::mixElseIfSwitches, "31"), "e(c(31))",
                "#n6:false{#n7:true{#n8:1{c}}};e");
        c("mix-else-if-switches-200", k -> k.start(Branches::mixElseIfSwitches, "200"), "e(d(b(200)))",
                "#n6:false{#n7:false{#n9:true{b};d}};e");
        c("mix-else-if-switches-201", k -> k.start(Branches::mixElseIfSwitches, "201"), "e(d(c(201)))",
                "#n6:false{#n7:false{#n9:false{c};d}};e");
        c("mix-pattern-in-loop", k -> k.start(Branches::mixPatternInLoop, List.of("99", "skip-1", "7", "hi")), "count 3",
                "value;#n10:Integer{#n11:true{a}};value;#n10:String{#n12:true{reject}};"
                        + "value;#n10:Integer{#n11:false{b}};value;#n10:String{#n12:false{c}}");
        c("mix-guard-in-arm-5", k -> k.start(Branches::mixGuardInArm, "5"), "e(b(a(5)))", "#n13:true{#n14:false{a;b}};e");
        c("mix-guard-in-arm-500", k -> k.start(Branches::mixGuardInArm, "500"), "rejected 500", "#n13:true{#n14:true{reject}}");
        c("mix-guard-in-arm-neg", k -> k.start(Branches::mixGuardInArm, "-1"), "e(c(-1))", "#n13:false{c};e");
        c("mix-guard-in-case-a1", k -> k.start(Branches::mixGuardInCase, "a1"), "e(b(a(a1)))", "#n15:a{a;#n16:false{b}};e");
        c("mix-guard-in-case-along", k -> k.start(Branches::mixGuardInCase, "along"), "e(rejected a(along))",
                "#n15:a{a;#n16:true{reject}};e");
        c("mix-guard-in-case-c1", k -> k.start(Branches::mixGuardInCase, "c1"), "e(c(c1))", "#n15:c{c};e");
        c("mix-ternary-in-arm-12", k -> k.start(Branches::mixTernaryInArm, "12"), "e(a(12))", "#n17:true{#n18:true{a}};e");
        c("mix-ternary-in-arm-13", k -> k.start(Branches::mixTernaryInArm, "13"), "e(b(13))", "#n17:true{#n18:false{b}};e");
        c("mix-ternary-in-arm-1", k -> k.start(Branches::mixTernaryInArm, "1"), "e(c(1))", "#n17:false{#n19:1{#n20:true{c}}};e");
        c("mix-ternary-in-arm-5", k -> k.start(Branches::mixTernaryInArm, "5"), "e(d(5))", "#n17:false{#n19:default{d}};e");
        // after a, the code may leave the inner loop and come round to #n21 again before b: the
        // calls cannot tell, so b is not drawn in #n21's case until #n21 is decided again for sure
        c("mix-loop-in-arm", k -> k.start(Branches::mixLoopInArm, "x"), "e(c(b(a(x))))",
                "#n21:true{#n22:0{a}};#n22:default{b};#n21:false{c};e");
        c("mix-task-condition-in-else-v", k -> k.start(Branches::mixTaskConditionInElse, "v1"), "e(a(v1))", "#n23:true{a};e");
        c("mix-task-condition-in-else-y", k -> k.start(Branches::mixTaskConditionInElse, "y1"), "e(b(y1))",
                "#n23:false{flag;#n24:true{b}};e");
        c("mix-task-condition-in-else-n", k -> k.start(Branches::mixTaskConditionInElse, "n1"), "e(c(n1))",
                "#n23:false{flag;#n24:false{c}};e");
        c("mix-deep-104", k -> k.start(Branches::mixDeep, "104"), "e(b(104))", "#n25:true{#n26:0,1{#n27:true{#n28:default{b}}}};e");
        c("mix-deep-105", k -> k.start(Branches::mixDeep, "105"), "e(a(105))", "#n25:true{#n26:0,1{#n27:true{#n28:0{a}}}};e");
        c("mix-deep-5", k -> k.start(Branches::mixDeep, "5"), "e(c(5))", "#n25:true{#n26:0,1{#n27:false{c}}};e");
        c("mix-deep-102", k -> k.start(Branches::mixDeep, "102"), "e(d(102))", "#n25:true{#n26:default{d}};e");
        c("mix-deep-neg", k -> k.start(Branches::mixDeep, "-3"), "e(-3)", "#n25:false{};e");
        c("mix-else-if-in-do-while", k -> k.start(Branches::mixElseIfInDoWhile, "3"), "e(c(a(b(3))))",
                "#n29:false{#n30:true{b}};#n29:true{a};#n29:false{#n30:false{c}};e");
    }

    // ---------------------------------------------------------------- the drawing, in markers

    static final Pattern MARK = Pattern.compile("//\\s*(#[a-z]+\\d+)");

    static Map<Integer, String> markers() throws Exception {
        Map<Integer, String> out = new HashMap<>();
        Path src = Path.of("native-examples/src/main/java/branches/Branches.java");
        List<String> lines = Files.readAllLines(src);
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = MARK.matcher(lines.get(i));
            if (m.find()) {
                out.put(i + 1, m.group(1));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static String render(List<Map<String, Object>> entries, Map<Integer, String> marks) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> e : entries == null ? List.<Map<String, Object>>of() : entries) {
            Map<String, Object> md = (Map<String, Object>) e.getOrDefault("metadata", Map.of());
            if (Boolean.TRUE.equals(md.get("preview")) || md.get("previewAfter") != null) {
                continue;
            }
            String type = (String) e.get("type");
            if ("SWITCH".equals(type)) {
                String site = String.valueOf(md.get("site"));
                int line = Integer.parseInt(site.substring(site.lastIndexOf(':') + 1));
                String key = String.valueOf(((Map<String, Object>) e.get("inputParameters")).get("switchCaseValue"));
                Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) e.get("decisionCases");
                List<Map<String, Object>> taken = cases.containsKey(key) ? cases.get(key) : (List<Map<String, Object>>) e.get("defaultCase");
                out.add(marks.getOrDefault(line, "?L" + line) + ":" + key + "{" + render(taken, marks) + "}");
            } else if ("TERMINATE".equals(type)) {
                out.add("throw");
            } else {
                out.add(String.valueOf(e.get("name")).replaceFirst("^br_", ""));
            }
        }
        return String.join(";", out);
    }

    @SuppressWarnings("unchecked")
    static Set<String> previews(List<Map<String, Object>> entries) {
        Set<String> out = new HashSet<>();
        for (Map<String, Object> e : entries == null ? List.<Map<String, Object>>of() : entries) {
            Map<String, Object> md = (Map<String, Object>) e.getOrDefault("metadata", Map.of());
            if (Boolean.TRUE.equals(md.get("preview")) || md.get("previewAfter") != null) {
                out.add((String) e.get("taskReferenceName"));
            }
            Object cases = e.get("decisionCases");
            if (cases instanceof Map<?, ?> m) {
                m.values().forEach(v -> out.addAll(previews((List<Map<String, Object>>) v)));
            }
            out.addAll(previews((List<Map<String, Object>>) e.get("defaultCase")));
        }
        return out;
    }

    // ---------------------------------------------------------------- running

    @SuppressWarnings("unchecked")
    static String run(Conductor conductor, Case k, Map<Integer, String> marks, Path dir) {
        WorkflowHandle<String> h = k.start.apply(conductor);
        String got;
        try {
            got = h.result(Duration.ofSeconds(60));
        } catch (Throwable t) {
            got = "THROW:" + t.getMessage();
        }
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + h.workflowId())
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        List<String> bad = new ArrayList<>();
        String status = String.valueOf(wf.get("status"));
        if (k.want.startsWith("FAILED:")) {
            String why = String.valueOf(wf.get("reasonForIncompletion"));
            if (!"FAILED".equals(status) || !why.contains(k.want.substring(7))) {
                bad.add("want " + k.want + ", got " + status + " " + why);
            }
        } else if (!"COMPLETED".equals(status) || !k.want.equals(got)) {
            bad.add("result: want " + k.want + ", got " + status + " " + got);
        }
        List<Map<String, Object>> def = (List<Map<String, Object>>) ((Map<String, Object>) wf.get("workflowDefinition")).get("tasks");
        String tree = render(def, marks);
        if (!k.tree.equals(tree)) {
            bad.add("drawing:\n        want " + k.tree + "\n        got  " + tree);
        }
        Set<String> previews = previews(def);
        for (Map<String, Object> t : (List<Map<String, Object>>) wf.get("tasks")) {
            if (previews.contains(t.get("referenceTaskName"))) {
                bad.add("preview " + t.get("referenceTaskName") + " ran");
            }
        }
        try {
            Path p = dir.resolve(k.id + ".json");
            Files.write(p, new ObjectMapper().writeValueAsBytes(wf));
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                DurableReplay.assertCompatible(Branches.class, p);
            }
        } catch (Throwable t) {
            bad.add("replay: " + t.getMessage());
        }
        String link = "http://localhost:5173/execution/" + h.workflowId();
        return (bad.isEmpty() ? "PASS " : "FAIL ") + link + (bad.isEmpty() ? "" : "\n      " + String.join("\n      ", bad));
    }

    public static void main(String[] args) throws Exception {
        defs();
        Map<Integer, String> marks = markers();
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Branches.class).tasks(new BranchTasks()).start();
        Path dir = Files.createTempDirectory("branches");
        String only = args.length > 0 ? args[0] : null;
        ExecutorService pool = Executors.newFixedThreadPool(6);
        Map<String, Future<String>> results = new LinkedHashMap<>();
        for (Case k : CASES) {
            if (only == null || k.id.matches(only)) {
                results.put(k.id, pool.submit(() -> {
                    try {
                        return run(conductor, k, marks, dir);
                    } catch (Throwable t) {
                        return "FAIL (no execution)\n      " + t; // the run is reported, the others go on
                    }
                }));
            }
        }
        int pass = 0;
        int fail = 0;
        for (Map.Entry<String, Future<String>> e : results.entrySet()) {
            String r = e.getValue().get();
            if (r.startsWith("PASS")) {
                pass++;
            } else {
                fail++;
            }
            System.out.println((r.startsWith("PASS") ? "ok   " : "BAD  ") + e.getKey() + "  " + r.substring(5));
        }
        System.out.println("BRANCHES pass=" + pass + " fail=" + fail);
        pool.shutdown();
        conductor.close();
        client.shutdown();
        System.exit(fail == 0 ? 0 : 1);
    }
}
