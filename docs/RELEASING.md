# Releasing

Pushing a version tag builds a signed APK and publishes it as a GitHub release. The in-app updater reads the latest release, so users are offered the update the next time they open PoolTrack.

## Publish a release

1. Bump `appVersionName` in `app/build.gradle.kts` (e.g. `1.2.0`) and merge it to `main`. `versionCode` is derived from it.
2. Tag the merge commit and push the tag:

   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

The [Release workflow](../.github/workflows/release.yml) fails if the tag does not match `appVersionName`. The release notes are generated from the merged pull requests, and are what users read in the update prompt.

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
