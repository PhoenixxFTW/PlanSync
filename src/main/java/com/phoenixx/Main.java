package com.phoenixx;

import okhttp3.*;

import java.util.Properties;

/**
 * @author Junaid Talpur
 * @project PlanSync
 * @since 1:46 PM [08-02-2023]
 */
public class Main {



    public static void main(String[] args) throws Exception {
        System.out.println("Starting PlanSync...");

        final Properties oAuthProperties = new Properties();
        oAuthProperties.load(Main.class.getResourceAsStream("/auth.properties"));

        PlanSync planSync = new PlanSync(
                oAuthProperties.getProperty("tenantID"),
                oAuthProperties.getProperty("clientID"),
                oAuthProperties.getProperty("secretID"),
                oAuthProperties.getProperty("redirectUri"),
                oAuthProperties.getProperty("scope"));

        planSync.runApp();


        /*// Step 1: Build the authorization URL
        String authUrl = "https://login.microsoftonline.com/"+TENANT_ID+"/oauth2/v2.0/authorize?" +
                "client_id=" + CLIENT_ID +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope;

        // Step 2: Redirect the user to the authorization URL
        System.out.println("Open the following URL in the browser and grant consent: " + authUrl);

        // Step 3: Handle the response
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter the code from the query parameters of the redirect URL: ");
        String code = br.readLine();

        // Step 4: Exchange the authorization code for an access token
        String tokenUrl = "https://login.microsoftonline.com/"+TENANT_ID+"/oauth2/v2.0/token";
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("client_id", CLIENT_ID);
        tokenParams.put("redirect_uri", redirectUri);
        tokenParams.put("client_secret", SECRET_ID);
        tokenParams.put("code", code);
        tokenParams.put("grant_type", "authorization_code");

        HttpURLConnection tokenConnection = (HttpURLConnection) new URL(tokenUrl).openConnection();
        tokenConnection.setDoOutput(true);
        tokenConnection.setRequestMethod("POST");
        tokenConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder tokenRequest = new StringBuilder();
        for (Map.Entry<String, String> param : tokenParams.entrySet()) {
            if (tokenRequest.length() != 0) {
                tokenRequest.append("&");
            }
            tokenRequest.append(param.getKey());
            tokenRequest.append("=");
            tokenRequest.append(param.getValue());
        }

        tokenConnection.getOutputStream().write(tokenRequest.toString().getBytes());
        BufferedReader tokenReader = new BufferedReader(new InputStreamReader(tokenConnection.getInputStream()));
        StringBuilder tokenResponse = new StringBuilder();
        String line;
        while ((line = tokenReader.readLine()) != null) {
            tokenResponse.append(line);
        }

        JsonObject tokenJson = JsonParser.parseString(tokenResponse.toString()).getAsJsonObject();
        String accessToken = tokenJson.get("access_token").getAsString();*/

        /*String accessToken = "XXXXXX";
        System.out.println("GOT ACCESS TOKEN: " + accessToken);
        getUsers(accessToken);*/
        //getPlanner(accessToken);

        /*// Step 5: Make an API request
        URL apiUrl = new URL("https://graph.microsoft.com/v1.0/users");
        HttpURLConnection apiConnection = (HttpURLConnection) apiUrl.openConnection();
        apiConnection.setRequestMethod("GET");
        apiConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        apiConnection.setRequestProperty("Content-Type", "application/json");

        int responseCode = apiConnection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader apiReader = new BufferedReader(new InputStreamReader(apiConnection.getInputStream()));
            StringBuilder apiResponse = new StringBuilder();
            String line2;
            while ((line2 = apiReader.readLine()) != null) {
                apiResponse.append(line2);
            }
            System.out.println(apiResponse.toString());
        } else {
            System.out.println("Request failed with response code: " + responseCode + " DATA: " + apiConnection.getResponseMessage());
        }*/

        /*String apiUrl = "https://graph.microsoft.com/v1.0/me/planner/tasks";
        HttpURL

        URLConnection apiConnection = new URL(apiUrl).openConnection();
        apiConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        BufferedReader apiReader = new BufferedReader(new InputStreamReader(apiConnection.getInputStream()));
        StringBuilder apiResponse = new StringBuilder();
        while ((line = apiReader.readLine()) != null) {
            apiResponse.append(line);
        }

        System.out.println(apiResponse.toString());*/
    }

    private static void getUsers(String accessToken) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");

        HttpUrl mySearchUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("graph.microsoft.com")
                .addPathSegment("/v1.0/users")
                //.addQueryParameter("Authorization", "Bearer XXXXX")
                .build();

        Request request = new Request.Builder()
                .url(mySearchUrl)
                .method("GET", null)
                .addHeader("Authorization", "Bearer "+accessToken)
                .build();
        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());

        /*URL apiUrl = new URL("https://graph.microsoft.com/v1.0/users");
        HttpURLConnection apiConnection = (HttpURLConnection) apiUrl.openConnection();
        apiConnection.setRequestMethod("GET");
        apiConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        apiConnection.setRequestProperty("Content-Type", "application/json");

        int responseCode = apiConnection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader apiReader = new BufferedReader(new InputStreamReader(apiConnection.getInputStream()));
            StringBuilder apiResponse = new StringBuilder();
            String line;
            while ((line = apiReader.readLine()) != null) {
                apiResponse.append(line);
            }
            System.out.println(apiResponse.toString());
        } else {
            System.out.println("Request failed with response code: " + responseCode + " MSG: " + apiConnection.getResponseMessage());
        }*/
    }

    private static void getPlanner(String accessToken) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        HttpUrl mySearchUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("graph.microsoft.com")
                .addPathSegment("/v1.0/me/planner/plans")
                //.addQueryParameter("Authorization", "Bearer XXXXX")
                .build();

        Request request = new Request.Builder()
                .url(mySearchUrl)
                .method("GET", null)
                .addHeader("Authorization", "Bearer "+accessToken)
                .build();
        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }
}
