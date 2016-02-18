package com.checkmarx.jenkins.workflow;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jenkinsci.plugins.workflow.job.*;
import org.jenkinsci.plugins.workflow.cps.*;


public class CxScanWorkflowTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    private WorkflowJob getBaseJob(String jobName) throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, jobName);
        return job;
    }

    @Test
    public void CxScanBuilderWorkflowStepTest() throws Exception {
        WorkflowJob job = getBaseJob("builder");
        job.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  step([$class: 'CxScanBuilder', useOwnServerCredentials: false, projectName: 'testProject', projectId: 1, buildStep: 'Step1', presetSpecified: false, incremental: false, fullScansScheduled: false, fullScanCycle: 1, skipSCMTriggers: true, waitForResultsEnabled: false, vulnerabilityThresholdEnabled: false, highThreshold: 1, mediumThreshold: 1, lowThreshold: 1, generatePdfReport: false])\n"
                + "}"));
        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = job.scheduleBuild2(0);
        WorkflowRun futureResult = workflowRunQueueTaskFuture.get();
        jenkinsRule.assertBuildStatus(Result.FAILURE, futureResult);
    }
}