---
name: accept
model: claude-sonnet-5-5
allowed-tools: Read, Write, Edit, Bash
description: >-
  Write a failing integration test for the NEXT spec rule (Step 2 of the
  development process). Use after a spec in doc/specs/ is finalised and before
  any production code is written for that rule. One rule at a time — the test
  must fail for the right reason.
argument-hint: "<rule name> @doc/specs/<feature>.md"
---

Write a failing integration test for: $ARGUMENTS

Read CLAUDE.md for project conventions before writing anything.
Re-read the spec file to understand the full rule, its examples, and its counter-examples.

## Structure

One outer class per feature, named `<Feature>integrationTest`.
One `@Nested` inner class per rule — name it after the rule.
One `@Test` per example from the spec.

Use `@DisplayName` with the spec's exact business language:
- Class: the rule name
- Method: the "The one where…" text from the spec

## How to test

Call interface API. 
NEVER call services or domain objects directly —
this is an integration test, not a unit test.

Assert exact values from the spec examples.
For money: `.andExpect(jsonPath("$.amount").value("1.60"))`
or use `isEqualByComparingTo` with `BigDecimal`.

## What NOT to do

Do NOT write production code. The test MUST FAIL.
A passing test means you tested nothing.
Do NOT write tests for all rules at once.
One rule only — the one specified in the arguments.
Do NOT invent examples beyond what the spec provides.
The spec is the contract.
Do NOT use mocks in integration tests.
Wire the full stack: API interface + other layers.

## When you're done

Run the test. Confirm it fails for the RIGHT reason:
- Test passes → something is wrong, investigate

Report: which rule you tested, how many examples, and the failure reason.

STOP. Do not proceed to implementation.