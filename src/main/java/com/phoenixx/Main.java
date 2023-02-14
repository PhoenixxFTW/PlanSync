package com.phoenixx;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
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
        
        Map<String, String> groups = Maps.newHashMap();
        List<String> groupTotals = Lists.newArrayList(oAuthProperties.getProperty("groups").split(", "));
        for(String groupFull: groupTotals) {
            String[] arr = groupFull.split(";");
            groups.put(arr[0], arr[1]);
        }

        PlanSync planSync = new PlanSync(
                oAuthProperties.getProperty("clientID"),
                Sets.newHashSet(oAuthProperties.getProperty("scope").split(", ")),
                groups);

        planSync.runApp();
    }
}
