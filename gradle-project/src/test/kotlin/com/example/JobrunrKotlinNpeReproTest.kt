package com.example

import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Tests to reproduce JobRunr + Kotlin 2.1 NPE bug.
 *
 * Expected behavior (when bug is present):
 * ```
 * java.lang.NullPointerException: Cannot invoke "String.endsWith(String)" because "name" is null
 *     at org.jobrunr.jobs.details.JobDetailsBuilder.setMethodName(JobDetailsBuilder.java:114)
 *     at org.jobrunr.jobs.details.KotlinJobDetailsBuilder.<init>(KotlinJobDetailsBuilder.java:13)
 *     at org.jobrunr.jobs.details.KotlinJobDetailsFinder.<init>(KotlinJobDetailsFinder.java:38)
 * ```
 *
 * NOTE: If these tests pass, the bug does NOT reproduce with this setup.
 * The bug may be specific to Bazel compilation or a different environment.
 */
@SpringBootTest
class JobrunrKotlinNpeReproTest {

    @Autowired
    lateinit var jobScheduler: JobScheduler

    @Autowired
    lateinit var myService: MyService

    @Test
    fun `enqueue kotlin lambda - should throw NPE if bug present`() {
        // When bug is present, this throws NPE in KotlinJobDetailsFinder
        // If this succeeds, the bug does not reproduce
        jobScheduler.enqueue { myService.doSomething() }
    }

    @Test
    fun `enqueue typed kotlin lambda - should throw NPE if bug present`() {
        // When bug is present, this throws NPE in KotlinJobDetailsFinder
        // If this succeeds, the bug does not reproduce
        jobScheduler.enqueue<MyService> { service -> service.doSomething() }
    }

    @Test
    fun `enqueue method reference - should throw NPE if bug present`() {
        // When bug is present, this throws NPE in KotlinJobDetailsFinder
        // If this succeeds, the bug does not reproduce
        jobScheduler.enqueue(myService::doSomething)
    }
}
