package ai.gen.model;

public class JobStatus {

    private String itemUrl;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    private String jobName;
    private String status;
    private String createdDate;

    public JobStatus(String jobName, String itemName, String status, String createdDate) {
        this.jobName = jobName;
        this.itemUrl = itemName;
        this.status = status;
        this.createdDate = createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getItemUrl() {
        return itemUrl;
    }

    public void setItemUrl(String itemUrl) {
        this.itemUrl = itemUrl;
    }


    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

}
