# CLAUDE.md

## Project
Library that manages a Live Football World Cup Scoreboard. 

## Build & Run

Uses the Maven wrapper; Java 23 required (see `pom.xml` `<java.version>`).

```bash
./mvnw test                           # run all tests
./mvnw -Dtest=ClassName test          # run a single test class
./mvnw -Dtest=ClassName#method test   # run a single test method
./mvnw clean package                  # build the jar
```

## Coding Conventions

### Java 23
Records for value objects, sealed interfaces, pattern matching.
No Lombok — records replace it.

### Library development rules
- Keep the public API small and hide everything else. The API should be as small as possible. 
  Minimize the accessibility of everything. In practice: use package-private classes, put internals in an internal package, and don’t export it.
- Prefer immutability. Classes should be immutable unless there’s a good reason to do otherwise.
- Don’t make the caller do work the library could do. Don’t make the client do anything the module could do.
- Expose interfaces rather than concrete classes.
- Design and document classes for inheritance, or make them final.
- Design exceptions with care. His exception-design rules are to Favor Unchecked Exceptions and to Include 
  Failure-Capture Information in Exceptions. Avoid long parameter lists too, and overload methods sparingly.
- Apply Effective Java item by item: static factories, builders, the equals/hashCode contracts, 
  composition over inheritance, generics and enums.
- Follow Google’s Best Practices for Java Libraries (JLBP). This is the most complete checklist for library authors. 
  The key rules include JLBP-1: Minimize dependencies, JLBP-2: Minimize API surface, JLBP-3: Use semantic versioning, 
  JLBP-4: Avoid dependencies on unstable libraries and features, along with JLBP-15: Produce a BOM for multi-module projects, 
  JLBP-18: Only shade dependencies as a last resort and JLBP-19: Place each package in only one module. Of these, dropping a 
  dependency matters most, because nothing is as effective as removing a dependency completely.
- Log through a facade and never ship a logging backend. Embedded components such as libraries or frameworks should not declare 
  a dependency on any SLF4J binding but only depend on slf4j-api. Don’t configure logging either: libraries should invoke SLF4J 
  to log but should let the end-user configure the logging environment.
- Support the Java module system (JPMS). At minimum, set a stable Automatic-Module-Name in the manifest. 
  The module name is effectively part of your API—once you pick a name, changing it constitutes a breaking change. 
  Later, add a real module-info.java that exports only your API packages.
- Document thoroughly. Every public type and method needs Javadoc that covers 
  parameters, return values, exceptions, thread-safety and nullness.

## Development Process

Follow these steps for every feature. Do NOT skip steps. Each step is a skill — invoke it.

Step 1: Discovery — Run /discover.
Propose rules, surface questions with options, let the user decide.
Save draft spec to docs/specs/.
STOP. User reviews, edits, and annotates the spec.
Do NOT proceed if the spec has unresolved questions.
Re-read the final spec before continuing.

Step 2: Integration Test — Write test for the NEXT rule only.
@Nested = rule, test = example.
Complete Step 3 until this rule is GREEN before writing the next.

Step 3: TDD (Inner Loop) — RED → GREEN → REFACTOR.
Write ONE failing test. Minimum code to pass. Refactor.
Run ALL tests. STOP after each cycle.

Step 4: Review — Verify coverage, boundaries, no AI smells.
Update CLAUDE.md if new conventions emerged.

## Testing Standards
Integration tests live in .../integration/ in test folder.
Inline test data per test. No shared fixtures. 