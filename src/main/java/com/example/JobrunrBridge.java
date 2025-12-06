package com.example;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * Java bridge for JobRunr lambdas.
 *
 * Java lambdas use JavaJobDetailsFinder which works correctly,
 * avoiding the NPE in KotlinJobDetailsFinder that occurs with
 * rules_kotlin-compiled Kotlin lambdas.
 */
@Component
public class JobrunrBridge {

    private final JobScheduler jobScheduler;

    public JobrunrBridge(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    /**
     * Enqueue using an instance you already have.
     */
    public void enqueueDoSomething(MyService myService) {
        jobScheduler.enqueue(() -> myService.doSomething());
    }

    /**
     * IoC-style: resolve MyService from Spring when the job runs.
     */
    public void enqueueDoSomethingIoc() {
        jobScheduler.<MyService>enqueue(MyService::doSomething);
    }
}
