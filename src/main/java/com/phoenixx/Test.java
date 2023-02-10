package com.phoenixx;

import com.microsoft.aad.msal4j.DeviceCode;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.PlannerBucketCollectionPage;
import com.microsoft.graph.requests.PlannerPlanCollectionPage;
import com.microsoft.graph.requests.PlannerTaskCollectionPage;
import okhttp3.Request;

import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Junaid Talpur
 * @project PlanSync
 * @since 1:19 PM [09-02-2023]
 */
public class Test {

    private static String tenantID;
    private static String clientID;
    private static String secretID;
    private static String redirectUri;
    private static String scope;

    private static String accessToken;

    private final static String authLink = "https://login.microsoftonline.com/common/";

    public static void main(String[] args) throws Exception {
        final Properties oAuthProperties = new Properties();
        oAuthProperties.load(Main.class.getResourceAsStream("/auth.properties"));
        tenantID = oAuthProperties.getProperty("tenantID");
        clientID = oAuthProperties.getProperty("clientID");
        secretID = oAuthProperties.getProperty("secretID");
        redirectUri = oAuthProperties.getProperty("redirectUri");
        scope = oAuthProperties.getProperty("scope");

        /*accessToken = getUserAccessToken(Sets.newHashSet(
                "User.Read", "offline_access",
                "openid", "profile",
                "User.ReadBasic.All", "Tasks.Read",
                 "Tasks.Read.Shared",
                "Tasks.ReadWrite", "Tasks.ReadWrite.Shared"));*/
        accessToken = "eyJ0eXAiOiJKV1QiLCJub25jZSI6IjF3VW1IZzFFYldfMFFCakpLU2QzRE5TNFFiVkpLUnR5ZVFqc2dJTDY1cDQiLCJhbGciOiJSUzI1NiIsIng1dCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9jZGRjMTIyOS1hYzJhLTRiOTctYjc4YS0wZTVjYWNiNTg2NWMvIiwiaWF0IjoxNjc1OTc1NTMwLCJuYmYiOjE2NzU5NzU1MzAsImV4cCI6MTY3NTk3OTg1NCwiYWNjdCI6MCwiYWNyIjoiMSIsImFpbyI6IkFWUUFxLzhUQUFBQUt4ZXBXUG4rZ04xaWpidWJWaWd1c1k0ZWp3Tkxjb3RWQ3VwdmZSWU5wOTYvRGd1ZkI4eE40RG84K0NGd3lxbG5XZFo2M1d1MHJHTTM1WkpxQWVmRnc5UE5jMmtkakx3M3BRbk54UGNMcTdjPSIsImFtciI6WyJwd2QiLCJtZmEiXSwiYXBwX2Rpc3BsYXluYW1lIjoiUGxhblN5bmMiLCJhcHBpZCI6IjhhZGUyODdmLTcxOTEtNGUyMC04MzdiLTU2MjFmNmY0Y2Y0ZCIsImFwcGlkYWNyIjoiMCIsImNvbnRyb2xzIjpbImFwcF9yZXMiXSwiY29udHJvbHNfYXVkcyI6WyIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiXSwiZmFtaWx5X25hbWUiOiJUYWxwdXIiLCJnaXZlbl9uYW1lIjoiSnVuYWlkIiwiaWR0eXAiOiJ1c2VyIiwiaXBhZGRyIjoiMTQyLjE5OC43Ny4xODIiLCJuYW1lIjoiVGFscHVyLCBKdW5haWQgKE1QQlNEKSIsIm9pZCI6IjQ4YTAwNDZlLTU5ZjctNGExZC1hZTUzLWM4ODM3ZDg5OTgzNCIsIm9ucHJlbV9zaWQiOiJTLTEtNS0yMS0yODExNDY2NTc3LTQxMTQ3MjU1MTAtMjA5NTUzOTU5NS0yMDY3OTEiLCJwbGF0ZiI6IjE0IiwicHVpZCI6IjEwMDMyMDAyNTlGMDRCQTgiLCJyaCI6IjAuQVJ3QUtSTGN6U3FzbDB1M2lnNWNyTFdHWEFNQUFBQUFBQUFBd0FBQUFBQUFBQUFjQU5BLiIsInNjcCI6IkNhbGVuZGFycy5SZWFkIGVtYWlsIE1haWwuUmVhZCBvcGVuaWQgcHJvZmlsZSBUYXNrcy5SZWFkIFRhc2tzLlJlYWQuU2hhcmVkIFRhc2tzLlJlYWRXcml0ZSBUYXNrcy5SZWFkV3JpdGUuU2hhcmVkIFVzZXIuUmVhZCBVc2VyLlJlYWRCYXNpYy5BbGwiLCJzdWIiOiJQUFRmemlWN0JvT2hVTUlWMGpzaURiNXdwazVtek1FQTh6QWtuVHJ5VHZNIiwidGVuYW50X3JlZ2lvbl9zY29wZSI6Ik5BIiwidGlkIjoiY2RkYzEyMjktYWMyYS00Yjk3LWI3OGEtMGU1Y2FjYjU4NjVjIiwidW5pcXVlX25hbWUiOiJKdW5haWQuVGFscHVyQG9udGFyaW8uY2EiLCJ1cG4iOiJKdW5haWQuVGFscHVyQG9udGFyaW8uY2EiLCJ1dGkiOiJqQmJOMTFSTFIwdXpXMGhfeElwQkFBIiwidmVyIjoiMS4wIiwid2lkcyI6WyJiNzlmYmY0ZC0zZWY5LTQ2ODktODE0My03NmIxOTRlODU1MDkiXSwieG1zX3N0Ijp7InN1YiI6IjBmUWFCWXBDQlVFVllDU2k5X0xrZUg4eFhZTVZUQVl4eWx0UERUdTNGbXMifSwieG1zX3RjZHQiOjE0NzgyNjg4NzV9.JamFSnzmu4QgvQI8Gr6sjxpqAntJiWGkd5lWIpx3r0UgmhOZ20GI283wwvlJCR3DhK27O999txUuZK1yYqTGF72OlcutEoPaZUMPjWcE1dUPzyEslW5K31Kp2qijbN68LOa8vn6l3J8r16YFWstLWjJM92s-stEe2ztz9otDlr0MwBujU7CWxX1cKY6BBgpzGx_sEtQLsHBA1Z-A2vkq2mpTBHyC3VJ471A5YNCWamby-eYL5-EJj9MikgEDRJl-NHZ2StVXhsN5uVM4h0Ebc2z615BwgpPcPjzC31m26Vl2q1LOsfJBUpKLKkklcUd_nT-DG96QrxWGP5XXCSYKPw";
        System.out.println("Access token: " + accessToken);

        int choice = -1;
        Scanner input = new Scanner(System.in);
        while (choice != 0) {
            System.out.println("Please choose one of the following options:");
            System.out.println("0. Exit");
            System.out.println("1. Display access token");
            System.out.println("2. Display User");
            System.out.println("3. Display Planners");

            choice = input.nextInt();

            switch(choice) {
                case 0:
                    // Exit the program
                    System.out.println("Exiting...");
                    break;
                case 1:
                    // Display access token
                    System.out.println(accessToken);
                    break;
                case 2:
                    System.out.println("USER INFO: ");
                    User user = getUser(accessToken);
                    System.out.println(user);
                    break;
                case 3:
                    PlannerPlanCollectionPage plans = getGraphClient().groups("c383d3c7-a2c2-47b4-9e6e-b605e994fab3").planner().plans().buildRequest().get();
                    System.out.println("Total Planners: " + plans.getCount());
                    plans.getCurrentPage().forEach(plannerPlan -> {
                        System.out.println("* " + plannerPlan.title + " | " + plannerPlan.id);

                        // BUCKETS ARE ALWAYS NULL
                        if(plannerPlan.buckets != null) {
                            plannerPlan.buckets.getCurrentPage().forEach(plannerBucket -> {
                                System.out.println("    - " + plannerBucket.name + "("+plannerBucket.id+")");
                            });
                        }
                    });

                    //TODO Use direct link to planner instead -> https://graph.microsoft.com/v1.0/planner/plans/MH5eT0RU60KYVOVOCpX2Z2UAFl8O/buckets
                    System.out.println("ALL VARONIS: ");
                    PlannerBucketCollectionPage buckets = getGraphClient().groups("c383d3c7-a2c2-47b4-9e6e-b605e994fab3").planner().plans("MH5eT0RU60KYVOVOCpX2Z2UAFl8O").buckets().buildRequest().get();
                    buckets.getCurrentPage().forEach(plannerBucket -> {
                        System.out.println("- " + plannerBucket.name + " -> " + plannerBucket.id);
                    });

                    PlannerTaskCollectionPage tasks = getGraphClient().planner().plans("MH5eT0RU60KYVOVOCpX2Z2UAFl8O").tasks().buildRequest().get();
                    tasks.getCurrentPage().forEach(plannerTask -> {
                        System.out.println(plannerTask.title + " ->" + plannerTask.id);
                    });
                    /* // THIS IS BROKEN
                    PlannerBucketCollectionPage buckets = graphClient.planner().plans("LR4OcjU5tkmdUKdXdGdPM2UAFoy8").buckets()
                            .buildRequest()
                            .get();
                    // THIS IS BROKEN
                    buckets.getCurrentPage().forEach(plannerBucket -> {
                        System.out.println("- "+plannerBucket.name + " | " + plannerBucket.id + " -> Tasks: " + plannerBucket.tasks.getCurrentPage().size());
                    });*/
                    break;
                default:
                    System.out.println("Invalid choice");
            }


        }
    }

    private static GraphServiceClient<Request> graphClient = null;
    private static IAuthenticationProvider authProvider;
    private static void ensureGraphClient(String accessToken) {
        if (graphClient == null) {
            // Create the auth provider
            authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

            // Create default logger to only log errors
            DefaultLogger logger = new DefaultLogger();
            logger.setLoggingLevel(LoggerLevel.ERROR);

            // Build a Graph client
            graphClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .logger(logger)
                    .buildClient();
        }
    }

    public static GraphServiceClient<Request> getGraphClient() {
        ensureGraphClient(accessToken);
        return graphClient;
    }

    public static User getUser(String accessToken) {
        ensureGraphClient(accessToken);
        // GET /me to get authenticated user
        return graphClient.me().buildRequest().get();
    }

    public static String getUserAccessToken(Set<String> scopes) {
        if (clientID == null) {
            System.out.println("Client ID NULL @@");
            return null;
        }

        PublicClientApplication app;
        try {
            // Build the MSAL application object with app ID and authority
            app = PublicClientApplication.builder(clientID).authority(authLink).build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        // Create consumer to receive the DeviceCode object
        // This method gets executed during the flow and provides
        // the URL the user logs into and the device code to enter
        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            System.out.println(deviceCode.message());
        };

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
}
