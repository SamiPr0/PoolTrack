---
name: domain-model-conventions
description: How PoolTrack's data models and repositories are structured - DataStore + kotlinx.serialization, deriving state instead of storing it redundantly, and migrating legacy on-device data without a user-visible migration step. Read before adding a field to a model or a new repository.
---

# Domain model conventions for PoolTrack

## Storage: DataStore Preferences + JSON, not SQLite/Room

Every repository (`SubscriptionRepositoryLocal`, `EntryRepositoryLocal`)
stores its data as a single JSON-encoded list in one Jetpack DataStore
`Preferences` key, via `kotlinx.serialization`'s `Json.encodeToString` /
`decodeFromString`. There's no database. A new domain model follows the
same shape:

```kotlin
private val Context.xDataStore by preferencesDataStore(name = "x_prefs")
private val X_KEY = stringPreferencesKey("x_json")

override fun getXs(): Flow<List<X>> =
    context.xDataStore.data.map { prefs ->
      prefs[X_KEY]?.let { Json.decodeFromString<List<X>>(it) } ?: emptyList()
    }
```

Model classes are `@Serializable` data classes; see `android-build-notes`
for the `Instant`-as-epoch-millis pattern this requires.

## Derive state, don't store it redundantly

If a value can be computed from data you already store, compute it - don't
add a second stored field that has to be kept in sync. Two examples already
in this codebase:

- The reopen cooldown (`remainingSubscriptionCooldown`) is derived from the
  most recent `Entry`'s timestamp, not from a separately stored "last
  opened at" field. This means deleting the latest entry (e.g. to undo a
  misclick) immediately and correctly lifts the cooldown, with no extra code
  needed to keep a redundant field in sync.
- `HomeStats` and `Subscription`'s `isExpired`/`daysUntilExpiration` are
  computed functions/properties over the stored data, not cached fields.

Before adding a new field to a model, ask whether it's actually new
information or just a cached computation of existing fields - if the
latter, write a function/extension property instead.

## Migrating on-device data without a migration UI

There's no user-facing "migrating your data..." step and no reason to add
one for a personal-scale, single-device app like this. When a stored
format changes (e.g. `Entry` moving from plain newline-separated timestamps
to JSON when it gained `subscriptionId`), read both formats: try the new
key first, fall back to decoding the old key's format if the new one isn't
there yet, and only write the new format back to disk (deleting the old
key) the next time a write happens (`addEntry`/`deleteEntry`), not on every
read. See `EntryRepositoryLocal.readEntries` for the pattern. This means
existing on-device data survives a format change with zero special-casing
by the caller, and old data can sit unread indefinitely with no harm if the
user never triggers a write.

## Cross-repository references are just IDs

`Entry.subscriptionId` and `Subscription.id` are plain `String`s, not a
foreign-key type or a live reference. A `ViewModel` that needs to join them
(e.g. counting entries per subscription) does so itself by combining both
repositories' flows - see `entryCountsBySubscriptionId` in
`SubscriptionViewModel` and `activeSubscriptionUsedEntries` in
`HomeViewModel`. There's no cascading delete: deleting a `Subscription`
does not delete its `Entry` rows, so a count against a deleted subscription
id will just come back as orphaned data with nothing to show it against -
this is accepted as fine for now, not a bug to silently work around.
