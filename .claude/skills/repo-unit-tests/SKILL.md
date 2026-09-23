---
name: repo-unit-tests
description: Use when writing or fixing JVM unit tests (app/src/test) for a ViewModel or for model/ logic, e.g. a ViewModel that takes repository interfaces, or a pure function like computeHomeStats.
---

Follow AGENTS.md. This skill covers how to write the tests, not the task rules.

## Principles

- Tests never touch DataStore, Firebase, the clock or shared state.
  Every test must pass on its own and when run repeatedly.
- Inject a fake via the constructor. If the class under test has no seam,
  stop and report it instead of changing production code.
- Assert behaviour: full objects (data class equality), what was stored in
  the fake, and the exception thrown. A size check alone is not an assertion.
- If a test exposes a real bug, stop and report it. Never bend the test around it.

## Tooling (already in the project, do not add dependencies)

- The in-memory fakes in `app/src/test/.../utils/`: `FakeEntryRepository`,
  `FakeSubscriptionRepository`, `FakeAuthRepository`. Extend them rather than
  writing new ones.
- `MainDispatcherRule` (`@get:Rule val mainDispatcherRule = MainDispatcherRule()`)
  for any ViewModel test: `viewModelScope` needs `Dispatchers.Main`.
- `kotlinx-coroutines-test` for `runTest`, `mockk` when a real Android object
  (e.g. a `Context`) must be passed but is not used.
- Create a fresh fake and subject per test.
- Name tests `methodUnderTest_expectedBehavior_condition`.

## Testing a ViewModel

Pass the fakes to the constructor, act, then assert both the ViewModel's state
and what the fake stored:

```kotlin
@Test
fun onDeleteEntry_removesOnlyThatEntry() {
  val repository = FakeEntryRepository(listOf(older, newer))
  val viewModel = HistoryViewModel(repository)

  viewModel.onDeleteEntry(newer)

  assertEquals(listOf(older), repository.storedEntries)
  assertEquals(listOf(older), viewModel.entries.value)
}
```

## Time, time zones and locale

Functions that default to `Instant.now()` or `ZoneId.systemDefault()` take them
as parameters: pass fixed values. If the result depends on `Locale.getDefault()`
(e.g. which day a week starts on), set it in `@Before` and restore it in `@After`.

## Cases to cover

1. Happy path with several items: assert every field of every item, and the order.
2. Empty state.
3. State updates when the repository changes after the ViewModel is created.
4. Each action (`onXxx`) changes exactly what it should, and nothing else.
5. Boundaries: exact limits (e.g. the cooldown ending exactly now), zero and
   negative values, missing optional fields.

Skip a case only when the code path is unreachable, and explain why in a
one-line comment rather than writing a fake test.

The `*RepositoryLocal` classes call Firebase directly (`mirrorToFirestore`), so
they are not unit-testable on the JVM; cover them with instrumented tests.

When done, run the `definition-of-done` skill.
