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

    private final List<String> groupNames;
    private final Map<String, String> groups;

    private String accessToken;
    private final OkHttpClient client;

    private final GraphServiceClient<Request> graphClient;

    private final static String authLink = "https://login.microsoftonline.com/common/";

    public PlanSync(String clientID, Set<String> scope, Map<String, String> groups) {
        this.clientID = clientID;
        this.scope = scope;
        this.groups = groups;

        this.groupNames = Lists.newArrayList(this.groups.keySet());

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
        this.accessToken = "eyJ0eXAiOiJKV1QiLCJub25jZSI6Im9JdXlXd3FfOE0zeVhwSUtyZm9QaUJmaThyLUtocUtOeUExUW9GbFNWekEiLCJhbGciOiJSUzI1NiIsIng1dCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9jZGRjMTIyOS1hYzJhLTRiOTctYjc4YS0wZTVjYWNiNTg2NWMvIiwiaWF0IjoxNjc2NDE1ODE3LCJuYmYiOjE2NzY0MTU4MTcsImV4cCI6MTY3NjQyMDI2NCwiYWNjdCI6MCwiYWNyIjoiMSIsImFpbyI6IkFWUUFxLzhUQUFBQVBxa3BzK0E5VTlwZk9QZkxKYzU4Q1JtZDZpVStxWWlQRThSOWxWd1FFRDNVbmRDandZS1E2cjhHN2VlQjREaThsM1BFWTFtc250SXhaRkFFamwzNW5PNTM4MDU4d3dwWXMvRm9zN3c5QW00PSIsImFtciI6WyJwd2QiLCJtZmEiXSwiYXBwX2Rpc3BsYXluYW1lIjoiUGxhblN5bmMiLCJhcHBpZCI6IjhhZGUyODdmLTcxOTEtNGUyMC04MzdiLTU2MjFmNmY0Y2Y0ZCIsImFwcGlkYWNyIjoiMCIsImNvbnRyb2xzIjpbImFwcF9yZXMiXSwiY29udHJvbHNfYXVkcyI6WyIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiXSwiZmFtaWx5X25hbWUiOiJUYWxwdXIiLCJnaXZlbl9uYW1lIjoiSnVuYWlkIiwiaWR0eXAiOiJ1c2VyIiwiaXBhZGRyIjoiMTQyLjE5OC43Ny4xODIiLCJuYW1lIjoiVGFscHVyLCBKdW5haWQgKE1QQlNEKSIsIm9pZCI6IjQ4YTAwNDZlLTU5ZjctNGExZC1hZTUzLWM4ODM3ZDg5OTgzNCIsIm9ucHJlbV9zaWQiOiJTLTEtNS0yMS0yODExNDY2NTc3LTQxMTQ3MjU1MTAtMjA5NTUzOTU5NS0yMDY3OTEiLCJwbGF0ZiI6IjE0IiwicHVpZCI6IjEwMDMyMDAyNTlGMDRCQTgiLCJyaCI6IjAuQVJ3QUtSTGN6U3FzbDB1M2lnNWNyTFdHWEFNQUFBQUFBQUFBd0FBQUFBQUFBQUFjQU5BLiIsInNjcCI6IkNhbGVuZGFycy5SZWFkIGVtYWlsIE1haWwuUmVhZCBvcGVuaWQgcHJvZmlsZSBUYXNrcy5SZWFkIFRhc2tzLlJlYWQuU2hhcmVkIFRhc2tzLlJlYWRXcml0ZSBUYXNrcy5SZWFkV3JpdGUuU2hhcmVkIFVzZXIuUmVhZCBVc2VyLlJlYWRCYXNpYy5BbGwiLCJzdWIiOiJQUFRmemlWN0JvT2hVTUlWMGpzaURiNXdwazVtek1FQTh6QWtuVHJ5VHZNIiwidGVuYW50X3JlZ2lvbl9zY29wZSI6Ik5BIiwidGlkIjoiY2RkYzEyMjktYWMyYS00Yjk3LWI3OGEtMGU1Y2FjYjU4NjVjIiwidW5pcXVlX25hbWUiOiJKdW5haWQuVGFscHVyQG9udGFyaW8uY2EiLCJ1cG4iOiJKdW5haWQuVGFscHVyQG9udGFyaW8uY2EiLCJ1dGkiOiI4eVN1RkVNUVpVU0lBaUV0NmkwckFBIiwidmVyIjoiMS4wIiwid2lkcyI6WyJiNzlmYmY0ZC0zZWY5LTQ2ODktODE0My03NmIxOTRlODU1MDkiXSwieG1zX3N0Ijp7InN1YiI6IjBmUWFCWXBDQlVFVllDU2k5X0xrZUg4eFhZTVZUQVl4eWx0UERUdTNGbXMifSwieG1zX3RjZHQiOjE0NzgyNjg4NzV9.osQu2CyAwA-WH8zuqtoAqH5lOG2NR4Rnkfo8op5l-AVNBseC1awbLkIiVWbW7f6Q2mJJsO3-ChV3c6nQx6X94mNZzG0vGpDjtO_yLZU8qR7vJJvSZ_JxE1Df071Z5YYdMNXl0q3Vo_FpCEoTyQZb7UteVeZrepRAEVVi6_r5uj3bk1t3yv7ghccqwLU1QruptgaqWfDvvlJdOfii_afSFH27egWpy3OSDckYth1mK_eO6x9Cjn__K5UvSX7Ie4uBDOE9nQGGs2eUdJN_0X5X7kKEF3ZTZoJ3UmByER0H74fD0XgTa0K4IfaS5q0C1Jje41u8Byi2zMf18a9UJ04aGw";
        //this.accessToken = this.getUserAccessToken(this.scope);
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

            String groupName;
            String groupIDString;

            switch (choice) {
                case 0:
                    // Exit the program
                    System.out.println("Exiting...");
                    break;
                case 1:
                    System.out.println("Select a group to display planners for [Total: "+this.groups.size()+"] (0 to exit): ");
                    groupName = this.groupSelection();
                    groupIDString = this.groups.get(groupName);

                    PlannerPlanCollectionPage plans = this.graphClient.groups(groupIDString).planner().plans().buildRequest().get();
                    if(plans == null) {
                        return;
                    }

                    System.out.println("\nTotal Planners in " + groupName + ": " + plans.getCount());
                    for(int i = 0; i < plans.getCurrentPage().size(); i++) {
                        PlannerPlan plannerPlan = plans.getCurrentPage().get(i);
                        System.out.printf((i+1)+") %-10s (%-10s)\n",plannerPlan.title,plannerPlan.id);

                        System.out.println("\tAll buckets for planner: ");
                        PlannerBucketCollectionPage tasks = this.getPlannerBuckets(plannerPlan.id);
                        for(int j = 0; j < tasks.getCurrentPage().size(); j++) {
                            PlannerBucket bucket = tasks.getCurrentPage().get(j);
                            System.out.println("\t\t-> "+(j+1)+") " + bucket.name + " ("+bucket.id+")");
                        }
                    }
                    break;
                case 2:
                    System.out.println("======================== Planner Migration Menu ========================");
                    System.out.println("Select a SOURCE Planner group [Total: " + this.groups.size() + "] (0 to exit): ");
                    // Select the group for the source planner
                    groupName = this.groupSelection();
                    groupIDString = this.groups.get(groupName);

                    PlannerPlanCollectionPage allPlanners = this.graphClient.groups(groupIDString).planner().plans().buildRequest().get();
                    if(allPlanners == null) {
                        return;
                    }
                    System.out.println("\nSelect a SOURCE Planner [Total: " + allPlanners.getCount() + "] (0 to exit): ");
                    PlannerPlan sourcePlanner = this.plannerSelection(allPlanners);
                    if(sourcePlanner == null) {
                        return;
                    }

                    System.out.println("\nSelect a TARGET Planner group [Total: " + this.groups.size() + "] (0 to exit): ");
                    // Select the group for the target planner
                    groupName = this.groupSelection();
                    groupIDString = this.groups.get(groupName);

                    allPlanners = this.graphClient.groups(groupIDString).planner().plans().buildRequest().get();
                    if(allPlanners == null) {
                        return;
                    }

                    System.out.println("\nSelect a TARGET Planner [Total: " + allPlanners.getCount() + "] (0 to exit): ");
                    PlannerPlan targetPlanner = this.plannerSelection(allPlanners);
                    if(targetPlanner == null) {
                        return;
                    }

                    System.out.println("\nSelect a bucket from " + targetPlanner.title +": ");
                    PlannerBucketCollectionPage tasks = this.getPlannerBuckets(targetPlanner.id);
                    for(int j = 0; j < tasks.getCurrentPage().size(); j++){
                        PlannerBucket bucket = tasks.getCurrentPage().get(j);
                        System.out.println((j+1)+") " + bucket.name + " ("+bucket.id+")");
                    }

                    System.out.print("> ");
                    int targetBucketID = input.nextInt();
                    if(targetBucketID <= 0 || targetBucketID > tasks.getCurrentPage().size()) {
                        break;
                    }
                    input.nextLine(); //throw away the \n not consumed by nextInt()

                    // We get the target bucket at the given index (subtract 1 because they start from 1)
                    PlannerBucket targetBucket = tasks.getCurrentPage().get(targetBucketID - 1);

                    System.out.print("\nEnter a prefix to add to the moved tasks (leave empty for no prefix): ");
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
     * Allows the user to select a planner and returns it
     *
     * @param planners The given {@link PlannerPlanCollectionPage} for the group we're accessing
     * @return The selected {@link PlannerPlan}
     */
    private PlannerPlan plannerSelection(PlannerPlanCollectionPage planners) {
        for(int i = 0; i < planners.getCurrentPage().size(); i++) {
            PlannerPlan plannerPlan = planners.getCurrentPage().get(i);
            System.out.printf((i+1)+") %-10s (%-10s)\n",plannerPlan.title,plannerPlan.id);
        }

        System.out.print("> ");
        Scanner input = new Scanner(System.in);
        int targetPlannerID = input.nextInt();
        if(targetPlannerID <= 0 || targetPlannerID > planners.getCurrentPage().size()) {
            return null;
        }
        // We get the target planner at the given index (subtract 1 because they start from 1)
        return planners.getCurrentPage().get(targetPlannerID - 1);
    }

    /**
     * Allows the user to pick a group and returns its name
     *
     * @return {@link String} the groups name
     */
    private String groupSelection() {
        Scanner input = new Scanner(System.in);
        int count=0;
        for(String groupName: this.groups.keySet()) {
            System.out.printf((count+1)+") %-10s (%-10s)\n", groupName, this.groups.get(groupName));
            count++;
        }

        System.out.print("> ");
        int groupID = input.nextInt();
        if(groupID <= 0 || groupID > this.groups.size()) {
            return "NULL";
        }

        return this.groupNames.get(groupID - 1);
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
