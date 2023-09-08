package ai.gen.model;

import java.util.List;
import java.util.Map;

public class JobData {
    private String job_name;
    private String item_page_url;
    private String overall_score;
    private String metrics_prediction;
    private Map<String, ListingData> listings;

    private String jobStartedAt;

    public String getJobStartedAt() {
        return jobStartedAt;
    }

    public void setJobStartedAt(String jobStartedAt) {
        this.jobStartedAt = jobStartedAt;
    }

    public String getJobCompletedAt() {
        return jobCompletedAt;
    }

    public void setJobCompletedAt(String jobCompletedAt) {
        this.jobCompletedAt = jobCompletedAt;
    }

    private String jobCompletedAt;

    public String getJob_name() {
        return job_name;
    }

    public void setJob_name(String job_name) {
        this.job_name = job_name;
    }

    public String getItem_page_url() {
        return item_page_url;
    }

    public void setItem_page_url(String item_page_url) {
        this.item_page_url = item_page_url;
    }

    public String getOverall_score() {
        return overall_score;
    }

    public void setOverall_score(String overall_score) {
        this.overall_score = overall_score;
    }

    public String getMetrics_prediction() {
        return metrics_prediction;
    }

    public void setMetrics_prediction(String metrics_prediction) {
        this.metrics_prediction = metrics_prediction;
    }

    public Map<String, ListingData> getListings() {
        return listings;
    }

    public void setListings(Map<String, ListingData> listings) {
        this.listings = listings;
    }

    public static class ListingData {
        private String score;
        private List<AttributeData> attributes;

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public List<AttributeData> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<AttributeData> attributes) {
            this.attributes = attributes;
        }
    }

    public static class ReasonData {
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        private String title;
        private String score;
        private String reason;
    }

    public static class AttributeData {
        private String name;
        private String value;
        private List<ReasonData> issues;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public List<ReasonData> getIssues() {
            return issues;
        }

        public void setIssues(List<ReasonData> issues) {
            this.issues = issues;
        }
    }
}

