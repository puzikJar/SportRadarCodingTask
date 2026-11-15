---
paths:
 - "src/test/java/**"
---

You are editing test classes here.

- Tests are executable specifications.
- `@DisplayName` on every class and method. 
- Use `@Nested` for groping related tests.
- Use `@ParameterizedTest` to model data-driven tests.
- Integration tests -> *IT (integration tests, run during mvn verify)*.
- All other tests -> *Test (unit tests, run during mvn test)*.

NEVER recalculate expected values.
NEVER modify a test to make it pass.
NEVER write multiple tests before implementing. 