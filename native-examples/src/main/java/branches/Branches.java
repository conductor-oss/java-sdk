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

import java.util.List;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.InputParam;

/**
 * Every way plain Java can write an if or a switch, one workflow each. Each if or switch is
 * marked with a comment ({@code // #name}) that {@link Check} uses to read the drawing.
 */
public class Branches {

    private final BranchTasks tasks;

    public Branches(BranchTasks tasks) {
        this.tasks = tasks;
    }

    public enum Color { RED, GREEN, BLUE }

    public sealed interface Shape permits Circle, Square, Triangle {
        static Shape of(String kind) {
            return switch (kind) {
                case "circle" -> new Circle(1);
                case "square" -> new Square(2);
                default -> new Triangle(3);
            };
        }
    }

    public record Circle(int r) implements Shape {}

    public record Square(int side) implements Shape {}

    public record Triangle(int base) implements Shape {}

    // ---------------------------------------------------------------- if

    @Workflow(name = "br_if_only")
    public String ifOnly(@InputParam("x") String x) {
        String r = x;
        if (x.equals("1")) { // #i1
            r = tasks.a(r);
        }
        return tasks.d(r);
    }

    @Workflow(name = "br_if_else")
    public String ifElse(@InputParam("x") String x) {
        String r;
        if (x.equals("1")) { // #i2
            r = tasks.a(x);
        } else {
            r = tasks.b(x);
        }
        return tasks.d(r);
    }

    @Workflow(name = "br_else_if")
    public String elseIf(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        if (n == 1) { // #i3
            r = tasks.a(x);
        } else if (n == 2) { // #i4
            r = tasks.b(x);
        } else if (n == 3) { // #i5
            r = tasks.c(x);
        } else {
            r = tasks.d(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_else_if_no_else")
    public String elseIfNoElse(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r = x;
        if (n == 1) { // #i6
            r = tasks.a(r);
        } else if (n == 2) { // #i7
            r = tasks.b(r);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_nested_if")
    public String nestedIf(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        if (n > 1) { // #i8
            if (n > 2) { // #i9
                r = tasks.a(x);
            } else {
                r = tasks.b(x);
            }
        } else {
            r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_no_braces")
    public String noBraces(@InputParam("x") String x) {
        String r;
        if (x.equals("1")) // #i10
            r = tasks.a(x);
        else
            r = tasks.b(x);
        return tasks.e(r);
    }

    @Workflow(name = "br_task_condition")
    public String taskCondition(@InputParam("x") String x) {
        String r;
        if (tasks.flag(x)) { // #i11
            r = tasks.a(x);
        } else {
            r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_and_or")
    public String andOr(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        if ((n > 0 && n < 3) || n == 9) { // #i12
            r = tasks.a(x);
        } else {
            r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_negated")
    public String negated(@InputParam("x") String x) {
        boolean big = Integer.parseInt(x) > 2;
        String r;
        if (!big) { // #i13
            r = tasks.a(x);
        } else {
            r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_empty_then")
    public String emptyThen(@InputParam("x") String x) {
        String r = x;
        if (x.equals("1")) { // #i14
            // nothing to do for 1
        } else {
            r = tasks.b(r);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_guard_return")
    public String guardReturn(@InputParam("x") String x) {
        if (x.equals("1")) { // #i15
            return tasks.a(x);
        }
        String r = tasks.b(x);
        return tasks.c(r);
    }

    @Workflow(name = "br_guard_continue")
    public String guardContinue(@InputParam("orders") List<String> orders) {
        int shipped = 0;
        for (String order : orders) {
            if (order.startsWith("bad")) { // #i16
                tasks.reject(order);
                continue;
            }
            tasks.charge(order);
            tasks.ship(order);
            shipped++;
        }
        return "shipped " + shipped;
    }

    @Workflow(name = "br_guard_break")
    public String guardBreak(@InputParam("orders") List<String> orders) {
        int done = 0;
        for (String order : orders) {
            if (order.equals("stop")) { // #i17
                tasks.reject(order);
                break;
            }
            tasks.charge(order);
            done++;
        }
        return "done " + done;
    }

    @Workflow(name = "br_guard_throw")
    public String guardThrow(@InputParam("x") String x) {
        if (x.isEmpty()) { // #i18 a guard with no call: what runs after it is its false arm
            throw new IllegalArgumentException("empty input");
        }
        if (x.equals("bad")) { // #i19
            tasks.reject(x);
            throw new IllegalStateException("rejected " + x);
        }
        return tasks.a(x);
    }

    @Workflow(name = "br_ternary_one_line")
    public String ternaryOneLine(@InputParam("x") String x) {
        String r = x.equals("1") ? tasks.a(x) : tasks.b(x); // #i20 both arms on one line: plain code
        return tasks.e(r);
    }

    @Workflow(name = "br_ternary_lines")
    public String ternaryLines(@InputParam("x") String x) {
        String r = x.equals("1") // #i21
                ? tasks.a(x)
                : tasks.b(x);
        return tasks.e(r);
    }

    @Workflow(name = "br_instanceof")
    public String instanceOf(@InputParam("x") String x) {
        Object value = tasks.value(x);
        String r;
        if (value instanceof Integer i && i > 5) { // #i22
            r = tasks.a(String.valueOf(i));
        } else {
            r = tasks.b(String.valueOf(value));
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_if_in_loop")
    public String ifInLoop(@InputParam("x") String x) {
        String r = x;
        for (int i = 0; i < 3; i++) {
            if (i % 2 == 0) { // #i23
                r = tasks.a(r);
            } else {
                r = tasks.b(r);
            }
        }
        return r;
    }

    @Workflow(name = "br_return_both")
    public String returnBoth(@InputParam("x") String x) {
        String r = tasks.d(x);
        if (x.equals("1")) { // #i24
            return tasks.a(r);
        } else {
            return tasks.b(r);
        }
    }

    // ---------------------------------------------------------------- switch statements

    @Workflow(name = "br_switch_old")
    public String switchOld(@InputParam("x") String x) {
        String r;
        switch (Integer.parseInt(x)) { // #s1
            case 1:
                r = tasks.a(x);
                break;
            case 2:
                r = tasks.b(x);
                break;
            default:
                r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_stacked")
    public String switchStacked(@InputParam("x") String x) {
        String r;
        switch (Integer.parseInt(x)) { // #s2
            case 1:
            case 2:
                r = tasks.a(x);
                break;
            default:
                r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_fallthrough")
    public String switchFallthrough(@InputParam("x") String x) {
        String r = x;
        switch (Integer.parseInt(x)) { // #s3 case 1 falls into case 2: plain code
            case 1:
                r = tasks.a(r);
            case 2:
                r = tasks.b(r);
                break;
            default:
                r = tasks.c(r);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_arrow")
    public String switchArrow(@InputParam("x") String x) {
        String r = x;
        switch (Integer.parseInt(x)) { // #s4
            case 1 -> r = tasks.a(x);
            case 2 -> {
                r = tasks.b(x);
                r = tasks.c(r);
            }
            default -> r = tasks.d(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_labels")
    public String switchLabels(@InputParam("x") String x) {
        String r = x;
        switch (Integer.parseInt(x)) { // #s5
            case 1, 2 -> r = tasks.a(x);
            case 3, 4, 5 -> r = tasks.b(x);
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_default_first")
    public String switchDefaultFirst(@InputParam("x") String x) {
        String r;
        switch (Integer.parseInt(x)) { // #s6
            default:
                r = tasks.c(x);
                break;
            case 1:
                r = tasks.a(x);
                break;
            case 2:
                r = tasks.b(x);
                break;
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_no_default")
    public String switchNoDefault(@InputParam("x") String x) {
        String r = x;
        switch (Integer.parseInt(x)) { // #s7
            case 1 -> r = tasks.a(x);
            case 2 -> r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_empty_case")
    public String switchEmptyCase(@InputParam("x") String x) {
        String r = x;
        switch (Integer.parseInt(x)) { // #s8
            case 1 -> { }
            case 2 -> r = tasks.b(x);
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_string")
    public String switchString(@InputParam("x") String x) {
        String r = x;
        switch (x) { // #s9 "Aa" and "BB" have the same hash code
            case "Aa" -> r = tasks.a(x);
            case "BB" -> r = tasks.b(x);
            case "red", "green" -> r = tasks.c(x);
            default -> r = tasks.d(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_enum")
    public String switchEnum(@InputParam("x") String x) {
        Color color = tasks.color(x);
        String r = x;
        switch (color) { // #s10
            case RED -> r = tasks.a(x);
            case GREEN -> r = tasks.b(x);
            case BLUE -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_enum_qualified")
    public String switchEnumQualified(@InputParam("x") String x) {
        Color color = tasks.color(x);
        String r = x;
        switch (color) { // #s11
            case Color.RED -> r = tasks.a(x);
            case Color.GREEN, Color.BLUE -> r = tasks.b(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_char")
    public String switchChar(@InputParam("x") String x) {
        String r = x;
        switch (x.charAt(0)) { // #s12
            case 'a' -> r = tasks.a(x);
            case 'b' -> r = tasks.b(x);
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_boxed")
    public String switchBoxed(@InputParam("x") String x) {
        Integer n = Integer.valueOf(x);
        String r = x;
        switch (n) { // #s13
            case 1 -> r = tasks.a(x);
            case 2 -> r = tasks.b(x);
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_task_value")
    public String switchTaskValue(@InputParam("x") String x) {
        String r = x;
        switch (tasks.pick(x)) { // #s14
            case "left" -> r = tasks.a(x);
            case "right" -> r = tasks.b(x);
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_return")
    public String switchReturn(@InputParam("x") String x) {
        switch (Integer.parseInt(x)) { // #s15
            case 1:
                return tasks.a(x);
            case 2:
                return tasks.b(x);
        }
        return tasks.c(x);
    }

    @Workflow(name = "br_switch_in_loop")
    public String switchInLoop(@InputParam("orders") List<String> orders) {
        int shipped = 0;
        for (String order : orders) {
            switch (order.substring(0, 3)) { // #s16
                case "bad":
                    tasks.reject(order);
                    continue;
                case "vip":
                    tasks.charge(order);
                    tasks.ship(order);
                    break;
                default:
                    tasks.ship(order);
            }
            shipped++;
        }
        return "shipped " + shipped;
    }

    @Workflow(name = "br_switch_nested")
    public String switchNested(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r = x;
        switch (n / 10) { // #s17
            case 1 -> {
                switch (n % 10) { // #s18
                    case 1 -> r = tasks.a(x);
                    default -> r = tasks.b(x);
                }
            }
            default -> r = tasks.c(x);
        }
        return tasks.e(r);
    }

    // ---------------------------------------------------------------- switch expressions

    @Workflow(name = "br_switch_expression")
    public String switchExpression(@InputParam("x") String x) {
        String r = switch (Integer.parseInt(x)) { // #x1
            case 1 -> tasks.a(x);
            case 2 -> tasks.b(x);
            default -> tasks.c(x);
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_yield")
    public String switchYield(@InputParam("x") String x) {
        String r = switch (Integer.parseInt(x)) { // #x2
            case 1:
                yield tasks.a(x);
            default: {
                String t = tasks.b(x);
                yield tasks.c(t);
            }
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_expression_block")
    public String switchExpressionBlock(@InputParam("x") String x) {
        String r = switch (x) { // #x3
            case "1" -> tasks.a(x);
            case "2" -> {
                String t = tasks.b(x);
                yield tasks.c(t);
            }
            default -> x;
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_expression_enum")
    public String switchExpressionEnum(@InputParam("x") String x) {
        Color color = tasks.color(x);
        String r = switch (color) { // #x4 every constant covered: no default
            case RED -> tasks.a(x);
            case GREEN -> tasks.b(x);
            case BLUE -> tasks.c(x);
        };
        return tasks.e(r);
    }

    // ---------------------------------------------------------------- pattern matching

    @Workflow(name = "br_switch_sealed")
    public String switchSealed(@InputParam("x") String x) {
        Shape shape = Shape.of(x);
        String r = switch (shape) { // #p1
            case Circle c -> tasks.a("circle " + c.r());
            case Square s -> tasks.b("square " + s.side());
            case Triangle t -> tasks.c("triangle " + t.base());
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_guarded")
    public String switchGuarded(@InputParam("x") String x) {
        Object value = tasks.value(x);
        String r = switch (value) { // #p2
            case Integer i when i > 5 -> tasks.a("big " + i);
            case Integer i -> tasks.b("small " + i);
            case String s -> tasks.c("text " + s);
            default -> tasks.d("other");
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_null")
    public String switchNull(@InputParam("x") String x) {
        String s = x.equals("none") ? null : x;
        String r = switch (s) { // #p3
            case null -> tasks.a("null");
            case "1" -> tasks.b(s);
            default -> tasks.c(s);
        };
        return tasks.e(r);
    }

    @Workflow(name = "br_switch_null_default")
    public String switchNullDefault(@InputParam("x") String x) {
        String s = x.equals("none") ? null : x;
        String r = switch (s) { // #p4
            case "1" -> tasks.a(s);
            case null, default -> tasks.b(String.valueOf(s));
        };
        return tasks.e(r);
    }

    // ---------------------------------------------------------------- nested mixes

    @Workflow(name = "br_mix_if_in_case")
    public String mixIfInCase(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        switch (n / 10) { // #n1
            case 1:
                if (n % 2 == 0) { // #n2
                    r = tasks.a(x);
                } else {
                    r = tasks.b(x);
                }
                break;
            case 2:
                r = tasks.c(x);
                break;
            default:
                r = tasks.d(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_switch_in_if")
    public String mixSwitchInIf(@InputParam("x") String x) {
        String r = x;
        if (x.startsWith("v")) { // #n3
            switch (x) { // #n4
                case "v1" -> r = tasks.a(x);
                case "v2" -> r = tasks.b(x);
                default -> r = tasks.c(x);
            }
        } else {
            r = switch (x) { // #n5
                case "1" -> tasks.d(x);
                default -> x;
            };
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_else_if_switches")
    public String mixElseIfSwitches(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        if (n < 10) { // #n6
            r = tasks.a(x);
        } else if (n < 100) { // #n7
            r = switch (n % 3) { // #n8
                case 0 -> tasks.b(x);
                case 1 -> tasks.c(x);
                default -> tasks.d(x);
            };
        } else {
            if (n % 2 == 0) { // #n9
                r = tasks.b(x);
            } else {
                r = tasks.c(x);
            }
            r = tasks.d(r);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_pattern_in_loop")
    public String mixPatternInLoop(@InputParam("items") List<String> items) {
        int count = 0;
        for (String item : items) {
            Object v = tasks.value(item);
            switch (v) { // #n10
                case Integer i -> {
                    if (i > 50) { // #n11
                        tasks.a(item);
                    } else {
                        tasks.b(item);
                    }
                }
                case String s -> {
                    if (s.startsWith("skip")) { // #n12
                        tasks.reject(s);
                        continue;
                    }
                    tasks.c(s);
                }
                default -> { }
            }
            count++;
        }
        return "count " + count;
    }

    @Workflow(name = "br_mix_guard_in_arm")
    public String mixGuardInArm(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r;
        if (n > 0) { // #n13
            if (n > 100) { // #n14
                return tasks.reject(x);
            }
            r = tasks.a(x);
            r = tasks.b(r);
        } else {
            r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_guard_in_case")
    public String mixGuardInCase(@InputParam("x") String x) {
        String r = x;
        switch (x.substring(0, 1)) { // #n15
            case "a":
                r = tasks.a(x);
                if (x.length() > 3) { // #n16
                    r = tasks.reject(r);
                    break;
                }
                r = tasks.b(r);
                break;
            case "c":
                r = tasks.c(x);
                break;
            default:
                r = tasks.d(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_ternary_in_arm")
    public String mixTernaryInArm(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r = x;
        if (n > 10) { // #n17
            r = n % 2 == 0 // #n18
                    ? tasks.a(x)
                    : tasks.b(x);
        } else {
            switch (n) { // #n19
                case 1 -> r = n > 0 // #n20
                        ? tasks.c(x)
                        : tasks.d(x);
                default -> r = tasks.d(x);
            }
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_loop_in_arm")
    public String mixLoopInArm(@InputParam("x") String x) {
        String r = x;
        for (int i = 0; i < 2; i++) {
            if (i == 0) { // #n21
                for (int j = 0; j < 2; j++) {
                    switch (j) { // #n22
                        case 0 -> r = tasks.a(r);
                        default -> r = tasks.b(r);
                    }
                }
            } else {
                r = tasks.c(r);
            }
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_task_condition_in_else")
    public String mixTaskConditionInElse(@InputParam("x") String x) {
        String r;
        if (x.startsWith("v")) { // #n23
            r = tasks.a(x);
        } else if (tasks.flag(x)) { // #n24
            r = tasks.b(x);
        } else {
            r = tasks.c(x);
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_deep")
    public String mixDeep(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r = x;
        if (n >= 0) { // #n25
            switch (n % 4) { // #n26
                case 0, 1 -> {
                    if (n > 100) { // #n27
                        r = switch (n % 3) { // #n28
                            case 0 -> tasks.a(x);
                            default -> tasks.b(x);
                        };
                    } else {
                        r = tasks.c(x);
                    }
                }
                default -> r = tasks.d(x);
            }
        }
        return tasks.e(r);
    }

    @Workflow(name = "br_mix_else_if_in_do_while")
    public String mixElseIfInDoWhile(@InputParam("x") String x) {
        int n = Integer.parseInt(x);
        String r = x;
        do {
            if (n % 2 == 0) { // #n29
                r = tasks.a(r);
            } else if (n % 3 == 0) { // #n30
                r = tasks.b(r);
            } else {
                r = tasks.c(r);
            }
            n--;
        } while (n > 0);
        return tasks.e(r);
    }
}
