package com.phoenixx;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.aad.msal4j.DeviceCode;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.PlannerBucket;
import com.microsoft.graph.models.PlannerPlan;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.PlannerBucketCollectionPage;
import com.microsoft.graph.requests.PlannerPlanCollectionPage;
import com.microsoft.graph.requests.PlannerTaskCollectionPage;
import okhttp3.*;
import okio.Buffer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Junaid Talpur
 * @project PlanSync
 * @since 6:15 PM [08-02-2023]
 */
public class PlanSync {
    private final String clientID;
    private final Set<String> scope;
    private String accessToken;
    private final OkHttpClient client;

    private final GraphServiceClient<Request> graphClient;

    // This will need to be updated in the future if you want to change the Azure group
    private final static String GROUP_ID = "c383d3c7-a2c2-47b4-9e6e-b605e994fab3";

    private final static String authLink = "https://login.microsoftonline.com/common/";

    public PlanSync(String clientID, Set<String> scope) {
        this.clientID = clientID;
        this.scope = scope;

        this.client = new OkHttpClient().newBuilder().build();

        // Create default logger to only log errors
        DefaultLogger logger = new DefaultLogger();
        logger.setLoggingLevel(LoggerLevel.ERROR);

        // Create auth provider with the given access token
        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(this.accessToken);
        // Build the GraphServiceClient using the authProvider above
        this.graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).logger(logger).buildClient();
    }

    /**
     * Runs the main app process.
     *
     * @throws Exception IOException, etc
     */
    public void runApp() throws Exception {
        //this.accessToken = "eyJ0eXAiOiJKV1QiLCJub25jZSI6IjJ4NFQzb0V5Sm8yd1dfNE5ISmRxV1g1azBETl9jdmUyNHJUbWROeGp1cEkiLCJhbGciOiJSUzI1NiIsIng1dCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9jZGRjMTIyOS1hYzJhLTRiOTctYjc4YS0wZTVjYWNiNTg2NWMvIiwiaWF0IjoxNjc2Mzg3MDczLCJuYmYiOjE2NzYzODcwNzMsImV4cCI6MTY3NjM5MTQwOCwiYWNjdCI6MCwiYWNyIjoiMSIsImFpbyI6IkFUUUF5LzhUQUFBQXNIT2c1M1ZCODBzNVVPS2Ird0lFU2FBQy9xRSt5alFYTVBTUzBoZEZpZlI5ZlhraGtCSXNPMEV0WjgxRlA5Y3UiLCJhbXIiOlsicHdkIl0sImFwcF9kaXNwbGF5bmFtZSI6IlBsYW5TeW5jIiwiYXBwaWQiOiI4YWRlMjg3Zi03MTkxLTRlMjAtODM3Yi01NjIxZjZmNGNmNGQiLCJhcHBpZGFjciI6IjAiLCJmYW1pbHlfbmFtZSI6IlRhbHB1ciIsImdpdmVuX25hbWUiOiJKdW5haWQiLCJpZHR5cCI6InVzZXIiLCJpcGFkZHIiOiIyMDQuNDAuMTI5Ljc0IiwibmFtZSI6IlRhbHB1ciwgSnVuYWlkIChNUEJTRCkiLCJvaWQiOiI0OGEwMDQ2ZS01OWY3LTRhMWQtYWU1My1jODgzN2Q4OTk4MzQiLCJvbnByZW1fc2lkIjoiUy0xLTUtMjEtMjgxMTQ2NjU3Ny00MTE0NzI1NTEwLTIwOTU1Mzk1OTUtMjA2NzkxIiwicGxhdGYiOiIxNCIsInB1aWQiOiIxMDAzMjAwMjU5RjA0QkE4IiwicmgiOiIwLkFSd0FLUkxjelNxc2wwdTNpZzVjckxXR1hBTUFBQUFBQUFBQXdBQUFBQUFBQUFBY0FOQS4iLCJzY3AiOiJDYWxlbmRhcnMuUmVhZCBlbWFpbCBNYWlsLlJlYWQgb3BlbmlkIHByb2ZpbGUgVGFza3MuUmVhZCBUYXNrcy5SZWFkLlNoYXJlZCBUYXNrcy5SZWFkV3JpdGUgVGFza3MuUmVhZFdyaXRlLlNoYXJlZCBVc2VyLlJlYWQgVXNlci5SZWFkQmFzaWMuQWxsIiwic2lnbmluX3N0YXRlIjpbImlua25vd25udHdrIl0sInN1YiI6IlBQVGZ6aVY3Qm9PaFVNSVYwanNpRGI1d3BrNW16TUVBOHpBa25UcnlUdk0iLCJ0ZW5hbnRfcmVnaW9uX3Njb3BlIjoiTkEiLCJ0aWQiOiJjZGRjMTIyOS1hYzJhLTRiOTctYjc4YS0wZTVjYWNiNTg2NWMiLCJ1bmlxdWVfbmFtZSI6Ikp1bmFpZC5UYWxwdXJAb250YXJpby5jYSIsInVwbiI6Ikp1bmFpZC5UYWxwdXJAb250YXJpby5jYSIsInV0aSI6IjhPZE5pM2xQM0VHU3NrdEtsMThjQUEiLCJ2ZXIiOiIxLjAiLCJ3aWRzIjpbImI3OWZiZjRkLTNlZjktNDY4OS04MTQzLTc2YjE5NGU4NTUwOSJdLCJ4bXNfc3QiOnsic3ViIjoiMGZRYUJZcENCVUVWWUNTaTlfTGtlSDh4WFlNVlRBWXh5bHRQRFR1M0ZtcyJ9LCJ4bXNfdGNkdCI6MTQ3ODI2ODg3NX0.YY4_bcrFoew7NmFgjLhSdQ85xZycnnU7TW4ZYZNg0TkzCf3uquBQs64iHzXmQm34qC7zOisHFOc_3MAbrc6GqHMqhOwNoNK_2nIJ7WtKDnV9v_uOj3kwMtN1Xv7SJOikjR_bMCzKFfhO2U8qOfmt0o4N7Qz-SiJ_0rySuWtMfCdJP4QG1vz5iyp-velJdHE-mkhRIVqrDrP8Vxoq9o55MNGwSqxKt1uKRO0dH32AA0WtqR7cm77Ki-R9yW9zFx8yR93rG5Ty6DdBrYM2IBKTpS_bN_-Xvr9QO-K8roZPskuCoWzyy_2qrm4vpwUgTBLgyi4hI8BuoK7x238V-joBUA";
        this.accessToken = this.getUserAccessToken(this.scope);
        System.out.println("Retrieved access token: " + this.accessToken);

        int choice = -1;
        Scanner input = new Scanner(System.in);
        while (choice != 0) {
            System.out.println();
            System.out.println("======================== PlanSync Menu ========================");
            System.out.println("Please choose one of the following options: ");
            System.out.println("0. Exit");
            System.out.println("1. Display Planners");
            System.out.println("2. Migrate planner to bucket");
            System.out.print("> ");

            choice = input.nextInt();
            System.out.println();

            switch (choice) {
                case 0:
                    // Exit the program
                    System.out.println("Exiting...");
                    break;
                case 1:
                    PlannerPlanCollectionPage plans = this.graphClient.groups(GROUP_ID).planner().plans().buildRequest().get();
                    System.out.println("Total Planners: " + plans.getCount());
                    for(int i = 0; i < plans.getCurrentPage().size(); i++) {
                        PlannerPlan plannerPlan = plans.getCurrentPage().get(i);
                        System.out.printf((i+1)+") %-10s (%-10s)\n",plannerPlan.title,plannerPlan.id);

                        System.out.println("\tAll buckets for planner: ");
                        PlannerBucketCollectionPage tasks = this.getPlannerBuckets(plannerPlan.id);
                        for(int j = 0; j < tasks.getCurrentPage().size(); j++){
                            PlannerBucket bucket = tasks.getCurrentPage().get(j);
                            System.out.println("\t\t-> "+(j+1)+") " + bucket.name + " ("+bucket.id+")");
                        }
                    }
                    break;
                case 2:
                    System.out.println("======================== Planner Migration Menu ========================");
                    PlannerPlanCollectionPage allPlanners = this.graphClient.groups(GROUP_ID).planner().plans().buildRequest().get();
                    System.out.println("Total Planners: " + allPlanners.getCount());

                    for(int i = 0; i < allPlanners.getCurrentPage().size(); i++) {
                        PlannerPlan plannerPlan = allPlanners.getCurrentPage().get(i);
                        System.out.printf((i+1)+") %-10s (%-10s)\n",plannerPlan.title,plannerPlan.id);
                    }

                    System.out.print("\nEnter the ID of the source Planner (0 to exit): ");
                    int sourcePlannerID = input.nextInt();
                    if(sourcePlannerID <= 0 || sourcePlannerID > allPlanners.getCurrentPage().size()) {
                        break;
                    }
                    // We get the source planner at the given index (subtract 1 because they start from 1)
                    PlannerPlan sourcePlanner = allPlanners.getCurrentPage().get(sourcePlannerID - 1);

                    System.out.print("Enter the ID of the target Planner (0 to exit): ");
                    int targetPlannerID = input.nextInt();
                    if(targetPlannerID <= 0 || targetPlannerID > allPlanners.getCurrentPage().size()) {
                        break;
                    }
                    // We get the target planner at the given index (subtract 1 because they start from 1)
                    PlannerPlan targetPlanner = allPlanners.getCurrentPage().get(targetPlannerID - 1);

                    System.out.println("\nBuckets from " + targetPlanner.title +": ");
                    PlannerBucketCollectionPage tasks = this.getPlannerBuckets(targetPlanner.id);
                    for(int j = 0; j < tasks.getCurrentPage().size(); j++){
                        PlannerBucket bucket = tasks.getCurrentPage().get(j);
                        System.out.println((j+1)+") " + bucket.name + " ("+bucket.id+")");
                    }

                    System.out.print("\nEnter the ID of the target Bucket (0 to exit): ");
                    int targetBucketID = input.nextInt();
                    if(targetBucketID <= 0 || targetBucketID > tasks.getCurrentPage().size()) {
                        break;
                    }
                    input.nextLine(); //throw away the \n not consumed by nextInt()

                    // We get the target bucket at the given index (subtract 1 because they start from 1)
                    PlannerBucket targetBucket = tasks.getCurrentPage().get(targetBucketID - 1);

                    System.out.print("Enter a prefix to add to the moved tasks (leave empty for no prefix): ");
                    String titlePrefix = input.nextLine();
                    if(titlePrefix.isEmpty()) {
                        titlePrefix = "";
                    }

                    // Migration confirmation
                    System.out.println("\nAre you sure you want to move tasks from planner \"" + sourcePlanner.title + "\" to bucket \"" + targetBucket.name + "\" in planner \"" + targetPlanner.title +"\" with the prefix \"" + titlePrefix +"\"?");
                    System.out.print("(y/n): ");
                    String check = input.next();
                    if (!check.equalsIgnoreCase("y")) {
                        break;
                    }

                    // Create request to get all the tasks in the given planner
                    Request getReq = this.createGetReq("https://graph.microsoft.com", "/v1.0/planner/plans/"+sourcePlanner.id+"/tasks",
                            Maps.newHashMap(new ImmutableMap.Builder<String, String>()
                                    .put("Content-Type", "application/json")
                                    .put("Authorization", "Bearer " + this.accessToken).build()));
                    Response response = this.client.newCall(getReq).execute();

                    // Convert the response into an JSON object in order to parse the eTag value
                    JSONObject responseObj = new JSONObject(response.body().string());
                    JSONArray taskArray = responseObj.getJSONArray("value");
                    System.out.println();

                    // This is the max amount of requests we can fit inside one batch request
                    int MAX_BATCH_REQUESTS = 20;
                    List<JSONObject> taskList = Lists.newArrayList();
                    for(int i = 0; i < taskArray.length(); i++) {
                        taskList.add(taskArray.getJSONObject(i));
                    }

                    List<List<JSONObject>> partitions = Lists.partition(taskList, MAX_BATCH_REQUESTS);
                    System.out.println("Preparing to move " + taskArray.length() + " tasks in " + partitions.size() + " batch requests...");

                    int batchCount = 1;
                    // Portion our task list into subsets of n size (MAX_BATCH_REQUESTS)
                    for (List<JSONObject> partition : partitions) {
                        JSONObject updatedTasksObj = new JSONObject();
                        JSONArray updatedTasks = new JSONArray();

                        for (int i = 0; i < partition.size(); i++) {
                            JSONObject taskObject = partition.get(i);
                            // This eTag value is essentially used as a hash, we compare it with the new request each time
                            String eTag = taskObject.getString("@odata.etag");
                            // Clear off the surrounding characters
                            eTag = eTag.substring(2,39) + "\"";
                            // Get the task ID
                            String taskId = taskObject.getString("id");
                            String taskName = taskObject.getString("title");

                            JSONObject batchObject = new JSONObject();
                            batchObject.put("id", i);
                            batchObject.put("method", "PATCH");
                            batchObject.put("url", "/planner/tasks/"+taskId);

                            JSONObject taskBody = new JSONObject();
                            taskBody.put("bucketId", targetBucket.id);
                            taskBody.put("planId", targetPlanner.id);
                            if(!titlePrefix.isEmpty()) {
                                taskBody.put("title", titlePrefix+" - "+taskName);
                            }
                            batchObject.put("body", taskBody);

                            JSONObject headers = new JSONObject();
                            headers.put("Content-Type", "application/json");
                            headers.put("If-Match", eTag);
                            headers.put("Authorization", "Bearer " + this.accessToken);
                            batchObject.put("headers", headers);

                            updatedTasks.put(batchObject);
                        }
                        updatedTasksObj.put("requests", updatedTasks);
                        Request batchRequest = new Request.Builder()
                                .url("https://graph.microsoft.com/v1.0/$batch")
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Authorization", "Bearer " + this.accessToken)
                                .post(RequestBody.create(updatedTasksObj.toString(), MediaType.parse("application/json")))
                                .build();

                        // Execute the request
                        response = client.newCall(batchRequest).execute();
                        boolean failure = false;

                        // We CANNOT call response.body() more than once as OkHTTP stores the response in memory and if its too big, we get an error
                        String responseBody = response.body().string();
                        responseObj = new JSONObject(responseBody);
                        JSONArray responseArray = responseObj.getJSONArray("responses");

                        // This gets really messy so I apologize beforehand.
                        // This will only find ONE failed response, not others

                        int failureCode = -1;
                        int reqID = -1;
                        JSONObject failedRequest = null;
                        JSONObject failedResponse = null;
                        for(int i = 0; i < responseArray.length(); i++) {
                            JSONObject jsonObject = responseArray.getJSONObject(i);

                            int statusCode = jsonObject.getInt("status");
                            if(jsonObject.getInt("status") != 204) {
                                failure = true;

                                reqID = jsonObject.getInt("id");
                                // We loop through the updatedTasks array json object to find the request with the ID of the current object we're at
                                for(int j = 0; j < updatedTasks.length(); j++) {
                                    JSONObject currTask = updatedTasks.getJSONObject(j);
                                    // If the ID matches, that's the failed request
                                    if(currTask.getInt("id") == reqID) {
                                        failedRequest = currTask;
                                        failedResponse = jsonObject;
                                        failureCode = statusCode;
                                        break;
                                    }
                                }

                            }
                        }

                        if(response.isSuccessful() && !failure) {
                            System.out.println("Batch #"+batchCount + " successfully moved " + partition.size() + " tasks.");
                        } else {
                            failure = true;
                            System.out.println("Batch #"+batchCount + " had a failure while trying to move " + partition.size() + " tasks.");
                        }

                        if(failure) {
                            System.out.println("================================ ERROR LOG ================================");
                            System.out.println("Received status: " + failureCode + " on \nrequest: " + (failedRequest != null ? failedRequest.toString() : "Could not find request with ID: " + reqID) + " \nresponse: " + (failedResponse != null ? failedResponse.toString() : " Could not find response with id: " + reqID));
                            System.out.println("Full Batch Request [#" + batchCount +"]: " + bodyToString(batchRequest));
                            System.out.println("Full Batch Response [#"+ batchCount +"]: " + responseBody);
                            System.out.println("===========================================================================");
                        }

                        batchCount++;
                    }
                    break;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    /**
     * Create a get request to the given path, along with the headers
     *
     * @param host The URL this request is for
     * @param pathSeg The endpoint we're accessing
     * @param headers The headers for our request
     * @return Returns a new {@link Request} which can be executed later
     */
    private Request createGetReq(String host, String pathSeg, Map<String, String> headers) {
        // Build the URL we're creating the request for. Had to do it this way due to a OkHttpClient bug
        Request.Builder request = new Request.Builder().url(host+pathSeg).method("GET", null);
        headers.forEach(request::addHeader);

        return request.build();
    }

    /**
     * Create a POST request for the given path along with headers and a body.
     *
     * @param host The URL this request is for
     * @param pathSeg The endpoint we're accessing
     * @param headers The headers for our request
     * @param body The body of the request
     * @return The {@link Request} that was created with the given data.
     */
    private Request createPostPatchReq(String type, String host, String pathSeg, Map<String, String> headers, Map<String, String> body) {
        // Build the URL we're creating the request for. Had to do it this way due to a OkHttpClient bug
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https").host(host).addPathSegment(pathSeg);
        headers.forEach(urlBuilder::addQueryParameter);

        MultipartBody.Builder multiBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        body.forEach(multiBodyBuilder::addFormDataPart);

        return new Request.Builder().url(urlBuilder.build()).method(type, multiBodyBuilder.build()).build();
    }

    private PlannerBucketCollectionPage getPlannerBuckets(String plannerID) {
        return this.graphClient.planner().plans(plannerID).buckets().buildRequest().get();
    }

    private PlannerTaskCollectionPage getPlannerTasks(String plannerID) {
        return this.graphClient.planner().plans(plannerID).tasks().buildRequest().get();
    }

    /**
     * Generates a user access token by authorizing the user that submits the provided device code.
     *
     * @param scopes The scope of the application
     * @return {@link String} access token
     * @throws Exception Throws a {@link MalformedURLException} if the {@link #authLink} provided was incorrect.
     */
    @Nullable
    private String getUserAccessToken(Set<String> scopes) throws Exception {
        PublicClientApplication app = PublicClientApplication.builder(this.clientID).authority(authLink).build();

        // Create consumer to receive the DeviceCode object (Used to print out the code)
        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> System.out.println(deviceCode.message());

        // Request a token, passing the requested permission scopes
        IAuthenticationResult result = app.acquireToken(DeviceCodeFlowParameters.builder(scopes, deviceCodeConsumer).build()).exceptionally(ex -> {
            System.out.println("Failed to authenticate - " + ex.getMessage());
            return null;
        }).join();

        if (result != null) {
            return result.accessToken();
        }

        return null;
    }

    /**
     * Converts a given {@link Request} into a string.
     *
     * @param request The {@link Request} to convert.
     * @return The final {@link String}
     */
    private static String bodyToString(final Request request){
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
