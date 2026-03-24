# Tool Use Validation: Schema-Based Input and Output Checking

An agent generates a tool call to fetch weather data, but the arguments might be malformed (missing required city field, wrong type for units, invalid enum value). Even if the input is correct, the tool's output might be incomplete or out of range. Without validation at both ends, bad data silently propagates through the pipeline and produces unreliable results.

This workflow validates tool calls at both the input and output boundaries, using JSON-schema-style checks before execution and after.

## Pipeline Architecture

```
userRequest, toolName
         |
         v
  tv_generate_tool_call  (toolArgs, inputSchema, outputSchema)
         |
         v
  tv_validate_input      (isValid, validatedArgs, checks list)
         |
         v
  tv_execute_tool        (rawOutput with weather data, executionTimeMs=245)
         |
         v
  tv_validate_output     (isValid, validatedOutput, validationReport)
         |
         v
  tv_deliver             (formattedResult string, validationSummary)
```

## Worker: GenerateToolCall (`tv_generate_tool_call`)

Produces structured `toolArgs`: `city: "London"`, `country: "UK"`, `units: "metric"`, `include: ["temperature", "humidity", "wind", "forecast"]`. Also generates an `inputSchema` map defining the expected structure: `required: ["city", "country"]`, with `units` constrained to `enum: ["metric", "imperial"]` and `include` typed as `array` of strings. Separately generates an `outputSchema` requiring `temperature` (number), `humidity` (number), and `conditions` (string).

## Worker: ValidateInput (`tv_validate_input`)

Runs four validation checks returned as `List<Map<String, Object>>`: `required_fields` ("All required fields present: city, country"), `type_validation`, `enum_validation` ("units value 'metric' is in allowed enum values"), and `array_items`. All checks pass. Returns `isValid: "true"`, the `validatedArgs` (pass-through), and an empty `validationErrors` list.

## Worker: ExecuteTool (`tv_execute_tool`)

Returns raw weather data: `temperature: 14.2`, `humidity: 72`, `conditions: "partly cloudy"`, `windSpeed: 18.5`, `windDirection: "SW"`, and a 3-day `forecast` list with maps containing `day`, `high`, `low`, and `conditions` for Monday (cloudy, 16/11), Tuesday (sunny, 18/12), and Wednesday (rain, 15/10). Includes `provider: "weather_api_v3"` and `executionTimeMs: 245`.

## Worker: ValidateOutput (`tv_validate_output`)

Runs four output checks: `required_fields` (temperature, humidity, conditions present), `type_validation`, `range_validation` ("values within expected ranges"), and `format_validation` (timestamp and provider properly formatted). Returns `isValid: "true"` and a `validationReport` with the checks list, empty errors list, and timestamp.

## Worker: Deliver (`tv_deliver`)

Formats the validated output into a human-readable string: `"Weather in London: 14.2C, partly cloudy. Humidity: 72%. Wind: 18.5 km/h SW. 3-day forecast available."` Includes a `validationSummary`: "All input and output validations passed. 4 input checks and 4 output checks completed with zero errors."

## Tests

5 tests cover tool call generation, input validation, execution, output validation, and delivery.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
