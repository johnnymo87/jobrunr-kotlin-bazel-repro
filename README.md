# JobRunr + Kotlin 2.1 NPE Reproduction (Bazel)

Minimal Bazel reproduction of a NullPointerException in JobRunr 8.3.0 when enqueueing Kotlin lambdas with Kotlin 2.1.

## The Bug

When calling `jobScheduler.enqueue { myService.doSomething() }` from Kotlin code compiled with Bazel's `rules_kotlin`, JobRunr throws:

```
java.lang.NullPointerException: Cannot invoke "String.endsWith(String)" because "name" is null
    at org.jobrunr.jobs.details.JobDetailsBuilder.setMethodName(JobDetailsBuilder.java:114)
    at org.jobrunr.jobs.details.KotlinJobDetailsBuilder.<init>(KotlinJobDetailsBuilder.java:13)
    at org.jobrunr.jobs.details.KotlinJobDetailsFinder.<init>(KotlinJobDetailsFinder.java:38)
```

The same code compiled with Gradle works correctly.

## Environment

- Bazel 7.4.1 with Bzlmod
- rules_kotlin 2.1.0
- rules_jvm_external 6.6
- Kotlin 2.1.0
- Spring Boot 3.4.2
- JobRunr 8.3.0 + jobrunr-kotlin-2.1-support 8.3.0
- JDK 17
- H2 in-memory database

## Steps to Reproduce

1. Run the test with Bazel:

   ```bash
   bazel test //:JobrunrKotlinNpeReproTest
   ```

2. Expected: Test fails with NPE in `KotlinJobDetailsFinder`

## Key Finding

**The bug is NOT caused by the `java_parameters` flag.**

Initial hypothesis suggested that rules_kotlin's default `java_parameters = False` (vs Gradle's `-java-parameters` flag) was the cause. However, testing confirmed:

- Bug occurs with `java_parameters = False`
- Bug **also** occurs with `java_parameters = True`

The actual cause appears to be a deeper difference in how rules_kotlin compiles Kotlin bytecode compared to Gradle's kotlin-gradle-plugin.

## Comparison with Gradle

A companion Gradle project at `../jobrunr-kotlin-npe-repro/` has identical code but compiles with Gradle. The Gradle tests pass successfully.

```bash
# Gradle (works)
cd ../jobrunr-kotlin-npe-repro && ./gradlew test

# Bazel (NPE)
bazel test //:JobrunrKotlinNpeReproTest
```

## Files

```
jobrunr-kotlin-bazel-repro/
├── MODULE.bazel                    # Bazel module with rules_kotlin + rules_jvm_external
├── BUILD.bazel                     # Build targets
├── .bazelrc                        # Bazel configuration (remote JDK 17)
├── .bazelversion                   # Bazel version (7.4.1)
├── src/main/kotlin/com/example/
│   ├── Application.kt              # Spring Boot application
│   └── MyService.kt                # Simple service
├── src/main/resources/
│   └── application.yml             # Spring/JobRunr config
├── src/test/kotlin/com/example/
│   └── JobrunrKotlinNpeReproTest.kt  # Test that reproduces NPE
└── README.md
```

## Investigation Notes

JobRunr's `KotlinJobDetailsFinder` uses ASM to parse bytecode and extract job method details from Kotlin lambdas. The NPE occurs because `name` is null when calling `JobDetailsBuilder.setMethodName()`.

This suggests rules_kotlin produces bytecode that is structured differently from Gradle-compiled Kotlin, causing the ASM-based parsing to fail.

To investigate further, bytecode comparison between Gradle and Bazel builds would be useful:

```bash
# Compare lambda bytecode
javap -v -classpath bazel-bin/app.jar 'com.example.JobrunrKotlinNpeReproTest$...'
```
