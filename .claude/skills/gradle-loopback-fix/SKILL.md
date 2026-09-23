---
name: gradle-loopback-fix
description: >-
  Use when a Gradle or JVM command (./gradlew, java, or a git commit whose
  pre-commit hook runs Gradle, e.g. ktfmtCheck) fails with "Unable to establish
  loopback connection" or "SocketException: Invalid argument: connect" on
  Windows. Also use before running gradlew from the Claude desktop app on Windows.
---

# Gradle "Unable to establish loopback connection" on Windows

Known bug: https://github.com/anthropics/claude-code/issues/77508 (open).
Don't diagnose it from scratch.

## Why it happens

JDK 17+ on Windows opens an NIO Selector whose wakeup pipe is an AF_UNIX socket
file in `%TEMP%`. The Claude desktop app is MSIX-packaged, so Windows redirects
files its child processes write under `AppData`. The socket's `connect()` then
fails with EINVAL. On this machine, `%TEMP%` is `C:\Users\USERLE~1\AppData\Local\Temp`.
- `./gradlew --version` still works, because it opens no Selector.
- Disabling the sandbox does not help.
- The user's own terminal is not affected.

## Fix

Prefix the command so every JVM (launcher, daemon, workers) puts the socket outside AppData:

```bash
mkdir -p /c/jtmp
JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/jtmp" ./gradlew <task>
```

For a commit blocked by the ktfmt pre-commit hook, prefix `git commit` the same way,
or `export` the variable first in the same Bash call:

```bash
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/jtmp"
git commit -F - <<'EOF'
...
EOF
```

Rules:
- The folder must exist, be outside `AppData`, and have **no spaces** in its path.
  `JAVA_TOOL_OPTIONS` splits on spaces, so `$HOME` (`C:/Users/User LENOVO`) fails
  with "Unrecognized option".
- Shell state does not persist between Bash calls. Set the variable in every call that runs Gradle.
- If the sandbox blocks the write, rerun the command with the sandbox disabled.
  Never skip the hook (`--no-verify`) to get around this.
- Only apply this on Windows inside the Claude desktop app. On macOS, Linux or CI, or if
  Gradle already works, don't add the prefix.

## Verify

`Picked up JAVA_TOOL_OPTIONS: ...` is printed and the task exits 0. If it still fails:
1. Check that the prefix was applied in that same call.
2. Check that `C:/jtmp` exists.
3. Try instead `export TEMP='C:\jtmp' TMP='C:\jtmp'` (the other workaround from the issue).

Launching Android Studio from Claude's shell may still crash (issue comments).
Ask the user to start the IDE and the emulator themselves.
