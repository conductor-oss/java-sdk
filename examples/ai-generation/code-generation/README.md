# From Requirements to Validated, Tested Code

Requirements go in, working code comes out. The parser extracts entities (`["User", "Order"]`), operations (`["create", "read", "update"]`), and constraints (`["auth required", "pagination"]`). The generator produces files (`["routes/users.js", "routes/orders.js"]`) with test cases (`{name: "create_user", input: {email: "test@ex.com"}, expected: 201}`). The validator and tester verify the output.

## Workflow

```
requirements, language, framework
  -> cdg_parse_requirements -> cdg_generate_code -> cdg_validate -> cdg_test
```

## Tests

8 tests cover requirement parsing, code generation, validation, and testing.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
