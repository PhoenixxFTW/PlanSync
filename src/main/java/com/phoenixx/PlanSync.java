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

                        if(response.isSuccessful()) {
                            System.out.println("Batch #"+batchCount + " successfully moved " + partition.size() + " tasks.");
                        } else {
                            failure = true;
                            System.out.println("Batch #"+batchCount + " failed to move " + partition.size() + " tasks.");
                        }

                        responseObj = new JSONObject(response.body().string());
                        JSONArray responseArray = responseObj.getJSONArray("responses");


                        for(int i = 0; i < responseArray.length(); i++) {
                            JSONObject jsonObject = responseArray.getJSONObject(i);

                            if(jsonObject.getInt("status") != 204) {
                                failure = true;
                                System.out.println("Batch #"+batchCount + " may have had an failed response object! Check the JSON response below for any mishaps.");
                            }
                        }

                        if(failure) {
                            System.out.println("Batch Request [#" + batchCount +"]: " + bodyToString(batchRequest));
                            System.out.println("Batch Response [#"+ batchCount +"]: " + response.body().string());
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
