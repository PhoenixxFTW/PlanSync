package com.phoenixx;

import com.google.common.collect.ImmutableMap;
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

    private final String tenantID;
    private final String clientID;
    private final String secretID;

    private final String redirectUri;
    private final Set<String> scope;

    private String authCode;
    private String accessToken;
    private final OkHttpClient client;

    private final GraphServiceClient<Request> graphClient;

    private final static String authLink = "https://login.microsoftonline.com/common/";

    public PlanSync(String tenantID, String clientID, String secretID, String redirectUri, Set<String> scope) {
        this.tenantID = tenantID;
        this.secretID = secretID;
        this.clientID = clientID;
        this.redirectUri = redirectUri;
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
            switch (choice) {
                case 0:
                    // Exit the program
                    System.out.println("Exiting...");
                    break;
                case 1:
                    PlannerPlanCollectionPage plans = this.graphClient.groups("c383d3c7-a2c2-47b4-9e6e-b605e994fab3").planner().plans().buildRequest().get();
                    System.out.println("Total Planners: " + plans.getCount());
                    for(int i = 0; i < plans.getCurrentPage().size(); i++) {
                        PlannerPlan plannerPlan = plans.getCurrentPage().get(i);
                        System.out.printf((i+1)+") %-10s (%-10s)\n",plannerPlan.title,plannerPlan.id);

                        System.out.println("\tAll buckets for planner: ");
                        PlannerBucketCollectionPage tasks = this.getPlannerBuckets(plannerPlan.id);
                        for(int j = 0; j < tasks.getCurrentPage().size(); j++){
                            PlannerBucket bucket = tasks.getCurrentPage().get(j);
                            System.out.println("\t\t-> "+j+") " + bucket.name + " ("+bucket.id+")");
                        }
                    }
                    break;
                case 2:
                    System.out.println("Enter the ID of a Planner to migrate FROM (leave empty to exit): ");
                    String movingFromPlannerID = input.next();
                    if(movingFromPlannerID.isEmpty()) {
                        break;
                    }
                    System.out.println("Enter the ID of a Planner to migrate TO (leave empty to exit): ");
                    String movingToPlannerID = input.next();
                    if(movingToPlannerID.isEmpty()) {
                        break;
                    }
                    System.out.println("Enter the ID of a Bucket to move tasks TO (leave empty to exit): ");
                    String bucketID = input.next();
                    if(bucketID.isEmpty()) {
                        break;
                    }

                    /**
                     * PROCESS FOR MOVING TASKS
                     * 1) Get the Task from the planner via https://graph.microsoft.com/v1.0/planner/tasks/{planID}
                     * 2) Read in the latest eTag (for example "JzEtVGFzayAgQEBAQEBAQEBAQEBAQEBATCc=" from the response from step one
                     * 3) Create new json with new planID and bucketID
                     * 4) monies
                     * https://graph.microsoft.com/v1.0/planner/tasks/QnGVJNCY8k6ry4PEcZEZPGUACRFE
                     * https://graph.microsoft.com/v1.0/planner/plans/{plan-id}/tasks
                     */

                    // Create request to get all the tasks in the given planner
                    Request request = this.createGetReq("https://graph.microsoft.com", "/v1.0/planner/plans/"+movingFromPlannerID+"/tasks",
                            Maps.newHashMap(new ImmutableMap.Builder<String, String>()
                                    .put("Content-Type", "application/json")
                                    .put("Authorization", "Bearer " + this.accessToken).build()));
                    Response response = this.client.newCall(request).execute();
                    // TODO Loop through response data and replace all planner ID and bucketID's for each task to the new one

                    JSONObject responseObj = new JSONObject(response.body().string());
                    JSONArray taskArray = responseObj.getJSONArray("value");
                    for (int i = 0; i < taskArray.length(); i++) {
                        JSONObject taskObject = taskArray.getJSONObject(i);

                        String eTag = taskObject.getString("@odata.etag");
                        //System.out.println("ORIGINAL eTAG: " + eTag);
                        eTag = eTag.substring(2,39) + "\"";
                        //System.out.println("NEW ETAG: " + eTag);
                        String taskId = taskObject.getString("id");

                        MediaType mediaType = MediaType.parse("application/json");
                        RequestBody body = RequestBody.create(mediaType, "{\n  \"bucketId\": \""+bucketID+"\",\n  \"planId\": \""+movingToPlannerID+"\"\n}");
                        request = new Request.Builder()
                                .url("https://graph.microsoft.com/v1.0/planner/tasks/"+taskId)
                                .method("PATCH", body)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("If-Match", eTag)
                                .addHeader("Authorization", "Bearer " + this.accessToken)
                                .build();

                        System.out.println("REQUEST: " + request);

                        response = client.newCall(request).execute();
                        System.out.println("RESPONSE: " + response);
                        System.out.println("COMPLETED: " + i);
                    }
                    // ALL THIS WORKS
                    /*String testTaskID = "QnGVJNCY8k6ry4PEcZEZPGUACRFE";

                    String progressBucket = "XV66tDVdyEWUpb8DCAmw1WUAMsgz";
                    String completedBucket = "KPyI0ItWtUCVY4mMWLUwbWUAIh8S"; // Varonis completed bucket

                    Request getReq = this.createGetReq("https://graph.microsoft.com", "/v1.0/planner/tasks/"+testTaskID,
                            Maps.newHashMap(new ImmutableMap.Builder<String, String>()
                                    .put("Content-Type", "application/json")
                                    .put("Authorization", "Bearer " + this.accessToken).build()));
                    Response response = this.client.newCall(getReq).execute();

                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String eTag = jsonObject.getString("@odata.etag");
                    System.out.println("ORIGINAL eTAG: " + eTag);
                    eTag = eTag.substring(2,39) + "\"";

                    System.out.println("NEW ETAG: " + eTag);

                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, "{\n  \"bucketId\": \""+completedBucket+"\"\n}");
                    Request request = new Request.Builder()
                            .url("https://graph.microsoft.com/v1.0/planner/tasks/"+testTaskID)
                            .method("PATCH", body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("If-Match", eTag)
                            .addHeader("Authorization", "Bearer " + this.accessToken)
                            .build();

                    response = client.newCall(request).execute();

                    System.out.println("RESPONSE: " + response);*/

                    break;
                default:
                    System.out.println("Invalid choice");
            }
        }

        System.out.println("Access token: " + accessToken);
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
