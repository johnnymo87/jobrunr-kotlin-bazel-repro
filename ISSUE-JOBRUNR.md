# [BUG] NPE in KotlinJobDetailsBuilder when parsing Kotlin lambda compiled with Bazel rules_kotlin

## JobRunr Version
8.3.0 (with jobrunr-kotlin-2.1-support 8.3.0)

## JDK Version
OpenJDK 17.0.11 (Temurin)

## Your SQL / NoSQL database
H2 in-memory (but the bug occurs before any database interaction)

## What happened?

When enqueueing a Kotlin lambda in a project compiled with Bazel's `rules_kotlin`, JobRunr throws an NPE in `KotlinJobDetailsBuilder`. The identical code compiled with Gradle works correctly.

**Stack trace:**
```
java.lang.NullPointerException: Cannot invoke "String.endsWith(String)" because "name" is null
    at org.jobrunr.jobs.details.JobDetailsBuilder.setMethodName(JobDetailsBuilder.java:114)
    at org.jobrunr.jobs.details.JobDetailsBuilder.<init>(JobDetailsBuilder.java:41)
    at org.jobrunr.jobs.details.JobDetailsBuilder.<init>(JobDetailsBuilder.java:31)
    at org.jobrunr.jobs.details.KotlinJobDetailsBuilder.<init>(KotlinJobDetailsBuilder.java:13)
    at org.jobrunr.jobs.details.KotlinJobDetailsFinder.<init>(KotlinJobDetailsFinder.java:38)
    at org.jobrunr.jobs.details.JobDetailsAsmGenerator.toJobDetails(JobDetailsAsmGenerator.java:20)
```

All three Kotlin lambda forms trigger the NPE:
- `jobScheduler.enqueue { myService.doSomething() }`
- `jobScheduler.enqueue<MyService> { it.doSomething() }`
- `jobScheduler.enqueue(myService::doSomething)`

**Expected behavior:**
JobRunr should either:
1. Successfully parse the lambda bytecode from rules_kotlin, or
2. Fail with a clear error message indicating unsupported bytecode pattern (not an NPE)

The bytecode produced by rules_kotlin is valid - the application runs correctly. However, `KotlinJobDetailsFinder`'s ASM-based parsing fails to extract the method name from the lambda.

## How to reproduce?

**Reproduction repository:** https://github.com/[TODO]/jobrunr-kotlin-bazel-repro

The repo contains two builds of identical Kotlin code:
- `gradle-project/` - Gradle build (works)
- `bazel-project/` - Bazel build with rules_kotlin 2.1.0 (fails)

**To reproduce:**
```bash
# Gradle - works
cd gradle-project
./gradlew test
# Result: PASSES

# Bazel - fails
cd bazel-project
bazel test //:JobrunrKotlinNpeReproTest
# Result: NPE in KotlinJobDetailsFinder
```

**Minimal code that fails:**
```kotlin
@Service
class MyService {
    fun doSomething(): String = "done"
}

// In a test:
@SpringBootTest
class JobrunrKotlinNpeReproTest {
    @Autowired lateinit var jobScheduler: JobScheduler
    @Autowired lateinit var myService: MyService

    @Test
    fun `enqueue kotlin lambda`() {
        jobScheduler.enqueue { myService.doSomething() }  // NPE here
    }
}
```

**Environment:**
- Kotlin 2.1.0
- Spring Boot 3.4.2
- Bazel 7.4.1 + rules_kotlin 2.1.0

**What I've tried:**
1. Setting `java_parameters = True` in rules_kotlin - still fails
2. Matching `jvm_target = "17"` - still fails
3. Various `kotlinc_opts` combinations - still fails

The only difference is the build tool (Gradle vs Bazel/rules_kotlin).

**Workarounds found:**
1. JobRequest/JobRequestHandler API - bypasses bytecode analysis entirely
2. Java bridge - move the lambda to Java code so `JavaJobDetailsFinder` is used instead
