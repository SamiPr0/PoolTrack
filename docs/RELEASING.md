# Releasing

Releases are automatic. When `main` gets a new app version, the [Release workflow](../.github/workflows/release.yml) builds a signed APK and publishes it as a GitHub release. The in-app updater reads the latest release, and updates are mandatory: users must install it the next time they open PoolTrack.

## Publish a release

Bump `appVersionName` in `app/build.gradle.kts` (e.g. `1.2.0` → `1.3.0`) in a pull request and merge it. `versionCode` is derived from it. That's all. Once CI passes on `main`, the Release workflow:

1. skips if `v<version>` is already released, so merges that keep the version publish nothing;
2. refuses a version older than the latest release, which would break updates;
3. builds and signs the APK, checks it can sign in with Google, then tags the merge commit `v<version>` and publishes the release.

The release notes are generated from the merged pull requests, and are what users read on the update screen. If a release fails, fix the cause and use **Actions → Release → Run workflow** to retry the current `main`.

## Signing key

Every release must be signed with the **same key**. If the key changes, Android refuses to install the new APK over the old one and users have to uninstall first. Keep the keystore backed up and out of the repo.

To create one:

```bash
keytool -genkeypair -v -keystore release.jks -alias pooltrack -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # the value of RELEASE_KEYSTORE
```

## Repository secrets

Add these under Settings → Secrets and variables → Actions:

| Secret | Content |
| --- | --- |
| `GOOGLE_SERVICES` | `app/google-services.json`, base64-encoded |
| `RELEASE_KEYSTORE` | the `.jks` keystore, base64-encoded |
| `RELEASE_KEYSTORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | the key alias (`pooltrack` above) |
| `RELEASE_KEY_PASSWORD` | the key password |
