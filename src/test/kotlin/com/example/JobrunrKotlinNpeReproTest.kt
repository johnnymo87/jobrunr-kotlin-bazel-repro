package com.example

import org.jobrunr.scheduling.JobRequestScheduler
import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Tests demonstrating:
 * 1. The NPE bug with Kotlin lambdas under rules_kotlin
 * 2. Two working workarounds: JobRequest/JobRequestHandler and Java Bridge
 */
@SpringBootTest
class JobrunrKotlinNpeReproTest {

    @Autowired
    lateinit var jobScheduler: JobScheduler

    @Autowired
    lateinit var jobRequestScheduler: JobRequestScheduler

    @Autowired
    lateinit var myService: MyService

    @Autowired
    lateinit var jobrunrBridge: JobrunrBridge

    // =========================================================================
    // BUG: These tests demonstrate the NPE with Kotlin lambdas
    // =========================================================================

    @Test
    @Disabled("Expected to fail with NPE - demonstrates the bug")
    fun `BUG - enqueue kotlin lambda throws NPE`() {
        // This triggers NPE in KotlinJobDetailsFinder
        jobScheduler.enqueue { myService.doSomething() }
    }

    @Test
    @Disabled("Expected to fail with NPE - demonstrates the bug")
    fun `BUG - enqueue typed kotlin lambda throws NPE`() {
        jobScheduler.enqueue<MyService> { service -> service.doSomething() }
    }

    @Test
    @Disabled("Expected to fail with NPE - demonstrates the bug")
    fun `BUG - enqueue method reference throws NPE`() {
        jobScheduler.enqueue(myService::doSomething)
    }

    // =========================================================================
    // WORKAROUND 1: JobRequest/JobRequestHandler - avoids bytecode analysis
    // =========================================================================

    @Test
    fun `WORKAROUND 1 - JobRequest does not trigger NPE`() {
        // JobRequest/JobRequestHandler bypasses bytecode analysis entirely
        // JobRunr just calls the handler's run() method directly
        jobRequestScheduler.enqueue(DoSomethingJobRequest("test message"))
    }

    // =========================================================================
    // WORKAROUND 2: Java Bridge - Java lambdas use JavaJobDetailsFinder
    // =========================================================================

    @Test
    fun `WORKAROUND 2a - Java bridge with instance does not trigger NPE`() {
        // Lambda lives in Java code, so JavaJobDetailsFinder is used
        jobrunrBridge.enqueueDoSomething(myService)
    }

    @Test
    fun `WORKAROUND 2b - Java bridge IoC style does not trigger NPE`() {
        // Method reference in Java code, resolved by Spring at job execution time
        jobrunrBridge.enqueueDoSomethingIoc()
    }
}
