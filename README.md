# JobRunr + Kotlin 2.1 NPE Reproduction (Bazel vs Gradle)

Minimal reproduction of a NullPointerException in JobRunr 8.3.0 when enqueueing Kotlin lambdas with Kotlin 2.1 compiled by Bazel's rules_kotlin. The same code works fine when compiled with Gradle.

## The Bug

When calling `jobScheduler.enqueue { myService.doSomething() }` from Kotlin code compiled with Bazel's `rules_kotlin`, JobRunr throws:

```
java.lang.NullPointerException: Cannot invoke "String.endsWith(String)" because "name" is null
    at org.jobrunr.jobs.details.JobDetailsBuilder.setMethodName(JobDetailsBuilder.java:114)
    at org.jobrunr.jobs.details.KotlinJobDetailsBuilder.<init>(KotlinJobDetailsBuilder.java:13)
    at org.jobrunr.jobs.details.KotlinJobDetailsFinder.<init>(KotlinJobDetailsFinder.java:38)
```

## Quick Start

```bash
git clone https://github.com/johnnymo87/jobrunr-kotlin-bazel-repro.git
cd jobrunr-kotlin-bazel-repro

# Gradle - works (uses invokedynamic by default)
cd gradle-project && ./gradlew test
# Result: PASSES

# Bazel - also works (configured with x_lambdas="indy")
cd .. && bazel test //:JobrunrKotlinNpeReproTest
# Result: PASSES
```

**Note:** This repo now includes the fix (`x_lambdas = "indy"`). To reproduce the original bug, change `x_lambdas` and `x_sam_conversions` back to `"class"` in `BUILD.bazel`.

## Environment

- Bazel 7.4.1 with Bzlmod
- rules_kotlin 2.1.0
- rules_jvm_external 6.6
- Kotlin 2.1.0
- Spring Boot 3.4.2
- JobRunr 8.3.0 + jobrunr-kotlin-2.1-support 8.3.0
- JDK 17
- H2 in-memory database

## Project Structure

```
jobrunr-kotlin-bazel-repro/
├── gradle-project/                 # Gradle build (works)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/...
├── MODULE.bazel                    # Bazel module (root - fixed)
├── BUILD.bazel
├── src/main/kotlin/com/example/
│   ├── Application.kt
│   ├── MyService.kt
│   ├── DoSomethingJobRequest.kt    # Workaround 1
│   └── DoSomethingJobHandler.kt    # Workaround 1
├── src/main/java/com/example/
│   └── JobrunrBridge.java          # Workaround 2
├── src/test/kotlin/com/example/
│   └── JobrunrKotlinNpeReproTest.kt
├── ISSUE-JOBRUNR.md                # Bug report for JobRunr
└── ISSUE-RULES-KOTLIN.md           # Bug report for rules_kotlin
```

## Root Cause

The issue is caused by rules_kotlin's default compiler flags. By default, `kt_kotlinc_options` sets:

- `x_lambdas = "class"`
- `x_sam_conversions = "class"`

This tells the Kotlin compiler to generate lambdas as **anonymous inner classes** instead of using **`invokedynamic`** (which is the Kotlin 2.x default and what Gradle uses).

JobRunr's `KotlinJobDetailsFinder` expects `invokedynamic` lambdas with `SerializedLambda` support. When it encounters class-based lambdas, the method name lookup returns `null`, causing the NPE.

## The Fix

Set `x_lambdas = "indy"` and `x_sam_conversions = "indy"` in your `kt_kotlinc_options`:

```starlark
kt_kotlinc_options(
    name = "kotlinc_opts",
    jvm_target = "17",
    x_lambdas = "indy",
    x_sam_conversions = "indy",
)
```

This produces `invokedynamic` bytecode matching Gradle's output, and all JobRunr lambda patterns work correctly.

## Workarounds (Alternative Approaches)

If you can't use `x_lambdas = "indy"` (e.g., you need serializable lambdas), two workarounds avoid the NPE:

### 1. JobRequest/JobRequestHandler

Bypasses bytecode analysis entirely:

```kotlin
// Instead of:
jobScheduler.enqueue { myService.doSomething() }  // NPE

// Use:
jobRequestScheduler.enqueue(DoSomethingJobRequest("message"))  // Works
```

### 2. Java Bridge

Move the lambda to Java code so `JavaJobDetailsFinder` is used:

```kotlin
// Instead of:
jobScheduler.enqueue { myService.doSomething() }  // NPE

// Use:
jobrunrBridge.enqueueDoSomething(myService)  // Works
```

## Related Issues

- JobRunr: https://github.com/jobrunr/jobrunr/issues/1453
- rules_kotlin: https://github.com/bazelbuild/rules_kotlin/issues/1417
