package com.example

import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.springframework.stereotype.Component

/**
 * Handler for DoSomethingJobRequest.
 * JobRunr resolves this via Spring and calls run() - no bytecode analysis needed.
 */
@Component
class DoSomethingJobHandler(
    private val myService: MyService
) : JobRequestHandler<DoSomethingJobRequest> {

    @Job(name = "Do something job")
    override fun run(jobRequest: DoSomethingJobRequest) {
        myService.doSomething()
    }
}
