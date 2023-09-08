package ai.gen.web;

import ai.gen.model.JobStatus;
import ai.gen.request.JobRequest;
import ai.gen.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    JobService service;

    private final Set<String> usedJobNames = new HashSet<>();

    @PostMapping("/create")
    public String createJob(@RequestBody JobRequest jobRequest) {
        // Generate a unique job name (you can use UUID or any other method)
        String jobName = jobRequest.getJobName();

        if (jobName == null || jobName.isEmpty()) {
            // If no custom job name is provided, generate a unique one
            jobName = generateUniqueJobName();
        } else {
            // Check if the provided job name is unique
            if (!isJobNameUnique(jobName)) {
                return "Job name '" + jobName + "' is not unique. Please provide a unique job name.";
            }
        }


        // Start the background job processing (e.g., using threads, async processing, or a job queue)
        // You can use a service class to encapsulate the job processing logic
        service.processJobAsync(jobName, jobRequest.getItemUrl());

        return "Job created with name: " + jobName;
    }

    @GetMapping("/status/{jobName}")
    public String getJobStatus(@PathVariable String jobName) {
        // Check the status of the job (you can use a service to manage job status)
        // Return the job status (e.g., "Pending", "Completed", "Failed", etc.)
        String jobStatus = service.getJobStatus(jobName);
        return "Job " + jobName + " is " + jobStatus;
    }

    @GetMapping("/result/{jobName}")
    public String getJobResult(@PathVariable String jobName) {
        // Check if the job exists
        if (service.getJobStatus(jobName).equals("Completed")) {
            // If the job is completed, retrieve and return the result
            return service.getJobResult(jobName);
        } else {
            return "Job " + jobName + " is not yet completed or doesn't exist.";
        }
    }

    @GetMapping("/all")
    public List<JobStatus> getAllJobStatuses() {
        // Retrieve all job statuses from the JobService
        return service.getAllJobStatuses();
    }

    private String generateUniqueJobName() {
        // Implement your logic to generate a unique job name
        // For simplicity, we'll use a timestamp-based name
        return "Job_" + System.currentTimeMillis();
    }

    private boolean isJobNameUnique(String jobName) {
        // Check if the job name is unique by looking it up in the set
        synchronized (usedJobNames) {
            if (usedJobNames.contains(jobName)) {
                return false; // Job name is not unique
            } else {
                usedJobNames.add(jobName); // Add the job name to the set to mark it as used
                return true; // Job name is unique
            }
        }
    }

}

