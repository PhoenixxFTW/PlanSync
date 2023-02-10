package com.phoenixx;

import com.google.common.collect.Sets;

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
                oAuthProperties.getProperty("clientID"),
                Sets.newHashSet(oAuthProperties.getProperty("scope").split(", ")));

        planSync.runApp();
    }
}
