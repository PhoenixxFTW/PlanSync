# PlanSync
PlanSync is an application designed to mass migrate tasks from one Microsoft Planner to another using Microsoft's Graph API.

## How it works
PlanSync works by interfacing with Microsoft's Graph API. It loads OAuth properties from a properties file, then uses these properties to authenticate and interact with Microsoft's Graph API.

## Usage
To run the application, you will need a properties file named auth.properties in the resources directory. This file should include the following properties:

- clientID: The client ID for the Microsoft Graph API.
- groups: The groups you want to work with. This should be a comma-separated list of groups in the format GroupName;GroupID.
- scope: The permissions you need for the Microsoft Graph API. This should be a comma-separated list of permissions.

Example of auth.properties:
```props
clientID=YourClientID  
groups=GroupName1;GroupID1, GroupName2;GroupID2  
scope=offline_access, openid, profile, User.Read, User.ReadBasic.All, Tasks.Read, Tasks.Read.Shared, Tasks.ReadWrite, Tasks.ReadWrite.Shared  
```

After you have set up your auth.properties file, you can run the application. The application will present a menu with the following options:

0. Exit: Exit the application.
1. Display Planners: Show all the planners for a given group.
2. Migrate planner to bucket: Migrate tasks from one planner to another.
