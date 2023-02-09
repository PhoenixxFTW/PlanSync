package com.phoenixx;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;
import java.util.Map;

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
    private final String scope;

    private final OkHttpClient client;

    private String authCode;

    public PlanSync(String tenantID, String clientID, String secretID, String redirectUri, String scope) {
        this.tenantID = tenantID;
        this.secretID = secretID;
        this.clientID = clientID;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.client = new OkHttpClient().newBuilder().build();
    }

    /**
     * Runs the main app process
     *
     * @throws Exception IOException, etc
     */
    public void runApp() throws Exception {
        System.out.println("Open the following URL in a browser and grant consent: ");
        System.out.println(this.getAuthLink());
/*
        // Input the authorization code the user received into the console
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter the code from the query parameters of the redirect URL: ");
        String authCode = br.readLine();
        this.setAuthCode(authCode);

        Request request = createPostReq("login.microsoftonline.com", "/"+this.tenantID+"/oauth2/v2.0/token", Maps.newHashMap(new ImmutableMap.Builder<String, String>().put("Content-Type", "application/x-www-form-urlencoded").build()),
                Maps.newHashMap(new ImmutableMap.Builder<String, String>()
                        .put("client_id", clientID)
                        .put("redirect_uri", redirectUri)
                        .put("client_secret", secretID)
                        .put("code", authCode)
                        .put("grant_type", "authorization_code")
                        .build()));

        Response response = client.newCall(request).execute();
        // Parse the response and get the access token
        Map<String, Object> responseMap = new ObjectMapper().readValue(response.body().byteStream(), HashMap.class);
        String accessToken = (String) responseMap.get("access_token");*/
        String accessToken = "eyJ0eXAiOiJKV1QiLCJub25jZSI6IkY3MU8ySVRFNVVwS0dieldZeWNXdHFVM21UNmpVb2JMaEZ2YTNmYzF4cGsiLCJhbGciOiJSUzI1NiIsIng1dCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9";
        System.out.println("accessToken: " + accessToken);
        //TODO Refresh tokens
        Request userRequest = this.createGetReq("graph.microsoft.com","/v1.0/users", Maps.newHashMap(new ImmutableMap.Builder<String, String>()
                .put("Authorization", "Bearer " + accessToken)
                .put("Content-Type", "application/json")
                .build()));

        Response response = client.newCall(userRequest).execute();
        System.out.println("RESPONSE: " +response);
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
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https").host(host).addPathSegment(pathSeg);
        headers.forEach(urlBuilder::addQueryParameter);
        HttpUrl url = urlBuilder.build();

        return new Request.Builder().url(url).method("GET", null).build();
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
    private Request createPostReq(String host, String pathSeg, Map<String, String> headers, Map<String, String> body) {
        // Build the URL we're creating the request for. Had to do it this way due to a OkHttpClient bug
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https").host(host).addPathSegment(pathSeg);
        headers.forEach(urlBuilder::addQueryParameter);

        MultipartBody.Builder multiBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        body.forEach(multiBodyBuilder::addFormDataPart);

        return new Request.Builder().url(urlBuilder.build()).method("POST", multiBodyBuilder.build()).build();
    }

    /**
     * Generates a link that will be used to authorize the client.
     * @return The {@link String} / auth link
     */
    private String getAuthLink() {
        return "https://login.microsoftonline.com/" +
                this.tenantID +
                "/oauth2/v2.0/authorize?" +
                "client_id=" + this.clientID +
                "&redirect_uri=" + this.redirectUri +
                "&response_type=code" +
                "&scope=" + this.scope;
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

    /**
     * Stores the auth code locally.
     * @param authCode The local auth code.
     */
    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    /**
     * Gets the auth code.
     * @return {@link String}
     */
    public String getAuthCode() {
        return this.authCode;
    }
}
