# Kotlin lambda bytecode from rules_kotlin differs from Gradle, causing third-party bytecode parser (JobRunr) to fail

## Summary

Kotlin lambdas compiled with rules_kotlin produce bytecode structured differently from Gradle-compiled lambdas. While the bytecode is valid and runs correctly, it causes issues with libraries that parse Kotlin bytecode using ASM.

Specifically, JobRunr's `KotlinJobDetailsFinder` throws an NPE when trying to extract the method name from a rules_kotlin-compiled lambda, while the identical code compiled with Gradle works fine.

## Environment

- Bazel 7.4.1
- rules_kotlin 2.1.0
- rules_jvm_external 6.6
- Kotlin 2.1.0
- JDK 17

For comparison:
- Gradle 9.2.0 with kotlin-gradle-plugin 2.1.0 (works)

## Error

When JobRunr inspects a lambda's bytecode compiled by rules_kotlin:

```
java.lang.NullPointerException: Cannot invoke "String.endsWith(String)" because "name" is null
    at org.jobrunr.jobs.details.JobDetailsBuilder.setMethodName(JobDetailsBuilder.java:114)
    at org.jobrunr.jobs.details.KotlinJobDetailsBuilder.<init>(KotlinJobDetailsBuilder.java:13)
    at org.jobrunr.jobs.details.KotlinJobDetailsFinder.<init>(KotlinJobDetailsFinder.java:38)
```

## Reproduction

**Repository:** https://github.com/johnnymo87/jobrunr-kotlin-bazel-repro

The repo contains two builds of identical code:
- `gradle-project/` - Gradle build (works)
- Root directory - Bazel/rules_kotlin build (fails)

```bash
git clone https://github.com/johnnymo87/jobrunr-kotlin-bazel-repro.git
cd jobrunr-kotlin-bazel-repro

# Gradle - works
cd gradle-project && ./gradlew test  # PASSES

# Bazel - fails (from repo root)
cd .. && bazel test //:JobrunrKotlinNpeReproTest  # NPE
```

**BUILD.bazel:**
```starlark
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:core.bzl", "kt_kotlinc_options")

kt_kotlinc_options(
    name = "kotlinc_opts",
    jvm_target = "17",
    java_parameters = True,  # Tried both True and False - no difference
)

kt_jvm_library(
    name = "app",
    srcs = ["MyService.kt"],
    kotlinc_opts = ":kotlinc_opts",
    deps = [...],
)
```

**Kotlin code:**
```kotlin
// This lambda's bytecode differs between Gradle and rules_kotlin
jobScheduler.enqueue { myService.doSomething() }
```

## What I've tried

1. `java_parameters = True` in `kt_kotlinc_options` - no change
2. `jvm_target = "17"` matching Gradle - no change
3. Various `kotlinc_opts` combinations - no change

## Questions

1. Is rules_kotlin intentionally producing different lambda bytecode than the Gradle Kotlin plugin? For example, different K2/IR backend flags, invokedynamic settings, or debug metadata?

2. Is there a "Gradle-compatible" configuration for rules_kotlin that would produce matching bytecode?

3. If this difference is intentional/expected, is it documented anywhere?

## Notes

- I've also filed an issue with JobRunr: [link to be added]
- The bytecode is functionally valid - applications run correctly
- The issue is specifically with third-party bytecode parsing/introspection
- I have workarounds, so this isn't blocking me, but wanted to report for visibility
