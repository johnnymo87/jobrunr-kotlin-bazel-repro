package com.example

import org.jobrunr.jobs.lambdas.JobRequest
import org.jobrunr.jobs.lambdas.JobRequestHandler

/**
 * JobRequest that avoids bytecode analysis entirely.
 * This is the cleanest workaround for the rules_kotlin NPE issue.
 */
data class DoSomethingJobRequest(
    val message: String = "default"
) : JobRequest {
    override fun getJobRequestHandler(): Class<out JobRequestHandler<DoSomethingJobRequest>> =
        DoSomethingJobHandler::class.java
}
