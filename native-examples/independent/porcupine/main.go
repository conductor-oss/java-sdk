// Independent linearizability check of a durability bench run's updates, with Porcupine.
//
// Each execution's `bump` update is a fetch-and-increment on a counter: it returns the new value.
// A bump whose outcome the client did not learn (info) may or may not have happened. The
// execution's final `bumps` output is a read at the end. Porcupine searches for one order of all
// operations, consistent with real time, in which every returned value is the counter's value.
//
//	go run . <run-dir>
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"math"
	"os"
	"path/filepath"
	"sort"
	"time"

	"github.com/anishathalye/porcupine"
)

type input struct {
	Key  string
	Read bool
}

const unknown = -1

var model = porcupine.Model{
	Partition: func(history []porcupine.Operation) [][]porcupine.Operation {
		byKey := map[string][]porcupine.Operation{}
		for _, op := range history {
			k := op.Input.(input).Key
			byKey[k] = append(byKey[k], op)
		}
		keys := make([]string, 0, len(byKey))
		for k := range byKey {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		out := make([][]porcupine.Operation, 0, len(keys))
		for _, k := range keys {
			out = append(out, byKey[k])
		}
		return out
	},
	Init: func() interface{} { return 0 },
	Step: func(state, in, out interface{}) (bool, interface{}) {
		s, o := state.(int), out.(int)
		if in.(input).Read {
			return o == s, s
		}
		if o == unknown { // indefinite bump: if it is linearized at all, it incremented
			return true, s + 1
		}
		return o == s+1, o
	},
	DescribeOperation: func(in, out interface{}) string {
		if in.(input).Read {
			return fmt.Sprintf("read -> %d", out.(int))
		}
		return fmt.Sprintf("bump -> %d", out.(int))
	},
}

type line struct {
	T       int64           `json:"t"`
	Process string          `json:"process"`
	Type    string          `json:"type"`
	F       string          `json:"f"`
	Key     string          `json:"key"`
	Value   json.RawMessage `json:"value"`
}

func main() {
	dir := os.Args[1]
	f, err := os.Open(filepath.Join(dir, "history.jsonl"))
	if err != nil {
		panic(err)
	}
	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 1<<20), 64<<20)
	type open struct {
		key string
		t   int64
	}
	pending := map[string]open{}
	var ops []porcupine.Operation
	client := 0
	clientOf := map[string]int{}
	var last int64
	for scanner.Scan() {
		var l line
		if json.Unmarshal(scanner.Bytes(), &l) != nil || l.F != "bump" {
			continue
		}
		if l.T > last {
			last = l.T
		}
		switch l.Type {
		case "invoke":
			pending[l.Process] = open{l.Key, l.T}
		case "ok", "info", "fail":
			o, ok := pending[l.Process]
			if !ok {
				continue
			}
			delete(pending, l.Process)
			if l.Type == "fail" { // definitely did not happen
				continue
			}
			if _, seen := clientOf[l.Process]; !seen {
				clientOf[l.Process] = client
				client++
			}
			op := porcupine.Operation{ClientId: clientOf[l.Process], Input: input{Key: o.key}, Call: o.t}
			if l.Type == "ok" {
				var v struct{ Value int }
				if err := json.Unmarshal(l.Value, &v); err != nil {
					panic(err)
				}
				op.Output, op.Return = v.Value, l.T
			} else { // info: may have happened at any point after the call
				op.Output, op.Return = unknown, math.MaxInt64
				clientOf[l.Process] = client // that client is gone
				client++
			}
			ops = append(ops, op)
		}
	}
	// the final count of every completed execution, read after everything
	var final struct {
		ByKey map[string][]struct {
			Status string
			Output map[string]interface{}
		}
	}
	raw, err := os.ReadFile(filepath.Join(dir, "final.json"))
	if err != nil {
		panic(err)
	}
	if err := json.Unmarshal(raw, &final); err != nil {
		panic(err)
	}
	reads := 0
	for key, executions := range final.ByKey {
		if len(executions) == 0 || executions[0].Status != "COMPLETED" {
			continue
		}
		bumps, _ := executions[0].Output["bumps"].(float64)
		ops = append(ops, porcupine.Operation{ClientId: client, Input: input{Key: key, Read: true},
			Call: last + 1, Output: int(bumps), Return: last + 2})
		client++
		reads++
	}
	result, info := porcupine.CheckOperationsVerbose(model, ops, 5*time.Minute)
	fmt.Printf("operations: %d updates, %d final reads\n", len(ops)-reads, reads)
	switch result {
	case porcupine.Ok:
		fmt.Println("PORCUPINE: linearizable")
	case porcupine.Illegal:
		fmt.Println("PORCUPINE: NOT linearizable")
		viz := filepath.Join(dir, "porcupine.html")
		if out, err := os.Create(viz); err == nil {
			_ = porcupine.Visualize(model, info, out)
			out.Close()
			fmt.Println("visualization:", viz)
		}
		os.Exit(1)
	default:
		fmt.Println("PORCUPINE: unknown (timed out)")
		os.Exit(2)
	}
}
