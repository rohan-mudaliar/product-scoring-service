package ai.gen.service;

import ai.gen.model.JobData;
import ai.gen.model.JobStatus;
import ai.gen.utils.AppUtils;
import ai.gen.utils.FractionRatioConverter;
import ai.gen.utils.RatingAggregator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class JobService {

    private final Map<String, String> jobStatusMap = new ConcurrentHashMap<>();
    private final Map<String, String> jobResultMap = new ConcurrentHashMap<>();
    private final Map<String, String> jobUrlMap = new ConcurrentHashMap<>();
    private final Map<String, String> jobDateMap = new ConcurrentHashMap<>();


    private final String SCRAPE_SERVICE_ENDPOINT = "http://localhost:8000/scrapping/perform";
    private final String PRICING_SERVICE_ENDPOINT = "http://localhost:8070/getCompetitivePrice?brand=test&itemName=test&price=";

    private final String TRANSLATE_ENDPOINT = "http://localhost:5000/translate_text";
    private final String SCORE_TITLE_ENDPOINT = "http://localhost:5000/score_title";
    private final String SCORE_DESCRIPTION_ENDPOINT = "http://localhost:5000//score_product_description";

    private final RestTemplate restTemplate;

    private final ObjectMapper mapper;

    public JobService() {
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }



    @Async
    public void processJobAsync(String jobName, String itemUrl) {
        jobStatusMap.put(jobName, "Pending");
        jobUrlMap.put(jobName, itemUrl);
        jobDateMap.put(jobName, getCurrentTime());

        try {
            String jobResult = simulateJobProcessing(jobName, itemUrl);

            jobStatusMap.put(jobName, "Completed");
            jobResultMap.put(jobName, jobResult);

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();

            jobStatusMap.put(jobName, "Failed");

        }

        // Once the job is done, you can log or update the job status
        System.out.println("Job " + jobName + " is completed.");
    }

    public String getCurrentTime() {
        Instant now = Instant.now();

        // Create a DateTimeFormatter to format the timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz")
                .withZone(ZoneId.of("UTC"));

        // Format the timestamp as a readable string
        return formatter.format(now);
    }

    public List<JobStatus> getAllJobStatuses() {
        // Return a copy of the jobStatusMap with additional information like creation date
        List<JobStatus> statusWithCreationDate = new ArrayList<>();

        for (Map.Entry<String, String> entry : jobStatusMap.entrySet()) {
            String jobName = entry.getKey();
            String jobStatus = entry.getValue();

            // Create a copy of the JobStatus object and add the creation date
            JobStatus statusWithDate = new JobStatus(
                    jobName,
                    jobUrlMap.get(jobName),
                    jobStatus,
                    jobDateMap.get(jobName)
            );

            statusWithCreationDate.add(statusWithDate);
        }

        return statusWithCreationDate;
    }

    private String simulateJobProcessing(String jobName, String itemUrl) throws InterruptedException, ExecutionException {
        // Simulate a long-running job (replace with your actual processing logic)
        CompletableFuture<String> scrapeResult = performScrape(itemUrl);
        CompletableFuture<String> priceAnalysisResult = scrapeResult.thenCompose(this::getCompetitivePrice);

        while (scrapeResult.isDone() && priceAnalysisResult.isDone()) {
            System.out.println("scraping and pricing is not yet completed. Polling...");

            // Add a delay between polls (you can adjust the delay as needed)
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String pricingServiceResponse = priceAnalysisResult.get();
        String priceScore = "0/5";
        try {
            priceScore = RatingAggregator.comparePrices(pricingServiceResponse, Double.valueOf(getPriceFromScrapeResponse(scrapeResult.get())));
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject scrapeResultObj = new JSONObject(scrapeResult.get());
        CompletableFuture<String> translatedTitle = getTranslatedText(scrapeResultObj.getString("title"), "en");
        Thread.sleep(3000);
        CompletableFuture<String> translatedDescription = getTranslatedText(scrapeResultObj.getString("description"), "en");

        while (translatedTitle.isDone() && translatedDescription.isDone()) {
            System.out.println("translation is not yet completed. Polling...");

            // Add a delay between polls (you can adjust the delay as needed)
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String translatedTitleStr = "";
        try {
            JSONObject ttObj = new JSONObject(translatedTitle.get());
            translatedTitleStr = ttObj.optString("Translated_text");
        } catch (JSONException je) {
            je.printStackTrace();
        }

        String translatedDescStr = "";
        try {
            JSONObject tt2Obj = new JSONObject(translatedDescription.get());
            translatedDescStr = tt2Obj.optString("Translated_text");
        } catch (JSONException je) {
            je.printStackTrace();
        }

        CompletableFuture<String> titleScore = getScore(SCORE_TITLE_ENDPOINT, translatedTitleStr);
        CompletableFuture<String> titleDescription = getScore(SCORE_DESCRIPTION_ENDPOINT, translatedDescStr);

        while (titleScore.isDone() && titleDescription.isDone()) {
            System.out.println("translation is not yet completed. Polling...");

            // Add a delay between polls (you can adjust the delay as needed)
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


        JSONObject titleScoreObj = new JSONObject();
        try {
            titleScoreObj = new JSONObject(titleScore.get());
        } catch (JSONException je) {
            je.printStackTrace();
        }

        JSONObject descScoreObj = new JSONObject();
        try {
            descScoreObj = new JSONObject(titleDescription.get());
        } catch (JSONException je) {
            je.printStackTrace();
        }


        JobData responseData = new JobData();
        responseData.setJob_name(jobName);
        responseData.setItem_page_url(itemUrl);



        Map<String, JobData.ListingData> listings = new HashMap<>();

        JobData.AttributeData titleAttr = new JobData.AttributeData();
        titleAttr.setName("item-name");
        titleAttr.setValue(translatedTitleStr);

        List<JobData.ReasonData> reasonList = new ArrayList<>();

        String tileOverAllScore = "0/5";

        for (int idx = 0; idx < titleScoreObj.optJSONArray("data").length(); idx++) {
            JobData.ReasonData reasonData = new JobData.ReasonData();

            String titleSection = titleScoreObj.getJSONArray("data").getJSONObject(idx).optString("Section_Title");

            if (titleSection.equalsIgnoreCase("Overall")) {
                tileOverAllScore = FractionRatioConverter.convertToRatio(titleScoreObj.getJSONArray("data").getJSONObject(idx).optString("Score"));
            } else {
                reasonData.setReason(titleScoreObj.getJSONArray("data").getJSONObject(idx).optString("Reason"));
                reasonData.setTitle(titleScoreObj.getJSONArray("data").getJSONObject(idx).optString("Section_Title"));
                reasonData.setScore(titleScoreObj.getJSONArray("data").getJSONObject(idx).optString("Score"));
                reasonList.add(reasonData);
            }
        }
        titleAttr.setIssues(reasonList);

        JobData.AttributeData descAttr = new JobData.AttributeData();
        descAttr.setName("description");
        descAttr.setValue(translatedDescStr);

        List<JobData.ReasonData> reasonDescList = new ArrayList<>();

        String descOverAllScore = "0/5";

        for (int idx = 0; idx < descScoreObj.optJSONArray("data").length(); idx++) {
            JobData.ReasonData reasonData = new JobData.ReasonData();

            String descSection = descScoreObj.getJSONArray("data").getJSONObject(idx).optString("Section_Title");

            if (descSection.equalsIgnoreCase("Overall")) {
                descOverAllScore = FractionRatioConverter.convertToRatio(descScoreObj.getJSONArray("data").getJSONObject(idx).optString("Score"));
            } else {
                reasonData.setReason(descScoreObj.getJSONArray("data").getJSONObject(idx).optString("Reason"));
                reasonData.setTitle(descScoreObj.getJSONArray("data").getJSONObject(idx).optString("Section_Title"));
                reasonData.setScore(descScoreObj.getJSONArray("data").getJSONObject(idx).optString("Score"));
                reasonList.add(reasonData);
            }
        }
        descAttr.setIssues(reasonList);

        List<JobData.AttributeData> coAttr = new ArrayList<>();
        coAttr.add(titleAttr);
        coAttr.add(descAttr);

        JobData.ListingData coData = new JobData.ListingData();
        coData.setAttributes(coAttr);
        String coScore = RatingAggregator.aggregateRatings(List.of(tileOverAllScore, descOverAllScore));
        coData.setScore(coScore);

        listings.put("content-observability", coData);


        JobData.AttributeData priceAttr = new JobData.AttributeData();
        priceAttr.setName("competitor price");

        JSONObject pricingResponseObj = new JSONObject();
        try {
            pricingResponseObj = new JSONObject(pricingServiceResponse);
        } catch (JSONException je) {
            je.printStackTrace();
        }
        priceAttr.setValue(pricingResponseObj.toString());
        List<JobData.ReasonData> priceReasons = new ArrayList<>();
        priceAttr.setIssues(priceReasons);

        List<JobData.AttributeData> priceAttrList = new ArrayList<>();
        priceAttrList.add(priceAttr);

        JobData.ListingData priceData = new JobData.ListingData();
        priceData.setAttributes(priceAttrList);
        priceData.setScore(priceScore);

        listings.put("offering", priceData);

        responseData.setListings(listings);

        List<String> aggrScores = new ArrayList<>();
        aggrScores.add(priceScore);
        aggrScores.add(coScore);

        String overAllRating = RatingAggregator.aggregateRatings(aggrScores);

        responseData.setOverall_score(overAllRating);
        responseData.setMetrics_prediction(AppUtils.convertRatingToString(AppUtils.extractNumerator(overAllRating)));

        responseData.setJobStartedAt(jobDateMap.get(jobName));
        responseData.setJobCompletedAt(getCurrentTime());

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            return ow.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }

    }



    private CompletableFuture<String> performScrape(String itemUrl) {

        return CompletableFuture.supplyAsync(() -> {
            // Simulate API call with different processing times
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                JSONObject requestObj = new JSONObject();
                requestObj.put("url", itemUrl);
                String requestBody = requestObj.toString();
                // Create a POST request with the given request body and headers

                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        SCRAPE_SERVICE_ENDPOINT,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );


                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    return responseEntity.getBody();
                } else {
                    return "API Call failed for " + SCRAPE_SERVICE_ENDPOINT + " with status code: " + responseEntity.getStatusCode();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "API Call failed for " + SCRAPE_SERVICE_ENDPOINT;
            }
        });
    }

    private CompletableFuture<String> getTranslatedText(String text, String targetLang) {

        System.out.println("source text = " + text);
        return CompletableFuture.supplyAsync(() -> {
            // Simulate API call with different processing times
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                JSONObject requestObj = new JSONObject();
                requestObj.put("original_text", text);
                requestObj.put("target_language", targetLang);
                String requestBody = requestObj.toString();
                // Create a POST request with the given request body and headers

                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        TRANSLATE_ENDPOINT,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );


                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    System.out.println("translated text = " + responseEntity.getBody());
                    return responseEntity.getBody();
                } else {
                    return "API Call failed for " + TRANSLATE_ENDPOINT + " with status code: " + responseEntity.getStatusCode();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "API Call failed for " + TRANSLATE_ENDPOINT;
            }
        });
    }

    private CompletableFuture<String> getScore(String endpoint, String input) {

        return CompletableFuture.supplyAsync(() -> {
            // Simulate API call with different processing times
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                JSONObject requestObj = new JSONObject();
                requestObj.put("input", input);
                String requestBody = requestObj.toString();
                // Create a POST request with the given request body and headers

                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );


                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    return responseEntity.getBody();
                } else {
                    return "API Call failed for " + endpoint + " with status code: " + responseEntity.getStatusCode();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "API Call failed for " + endpoint;
            }
        });
    }

    private CompletableFuture<String> getCompetitivePrice(String scrapeResponse) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate API call with different processing times
            try {

                String price = getPriceFromScrapeResponse(scrapeResponse);
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(
                        PRICING_SERVICE_ENDPOINT + price,
                        String.class
                );


                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    return responseEntity.getBody();
                } else {
                    return "API Call failed for " + PRICING_SERVICE_ENDPOINT + " with status code: " + responseEntity.getStatusCode();
                }
            } catch (Exception e) {
                return "API Call failed for " + PRICING_SERVICE_ENDPOINT;
            }
        });
    }

    private String getPriceFromScrapeResponse(String scrapeResponse) {
        try {
            JSONObject scrapeJson = new JSONObject(scrapeResponse);
            return scrapeJson.optString("price", "0.0");
        } catch (Exception e) {
            return "0.0";
        }
    }


    public String getJobStatus(String jobName) {
        // Get the job status from the map
        return jobStatusMap.getOrDefault(jobName, "Job not found");
    }

    public String getJobResult(String jobName) {
        // Get the job result from the map
        return jobResultMap.getOrDefault(jobName, "Result not available");
    }
}
