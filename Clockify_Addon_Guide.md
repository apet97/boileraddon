logo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Introduction# Clockify Add-on basics# What is the structure
of an add-on?# Each add-on has three main elements:

Manifest Business logic UI (optional) Manifest describes the add-on's
capabilities and the way it integrates with the Clockify app.

Business logic represents the functionalities provided by an add-on.

UI is the visual representation of an add-on that is displayed to users.
Add-ons that don't contain a UI can also be developed.

How does add-on hosting infrastructure work?# Add-on resources are not
hosted by CAKE.com. You must host all the resources needed for an add-on
to function, including a manifest file, a database, a web server to
handle communication with Clockify and any other integral part of an
add-on e.g. UI.

You need to make sure that all the resources mentioned above are working
and accessible.

How does the add-on interact with the Clockify API?# An add-on interacts
with Clockify's API by supplying an authentication token as part of the
X-Addon-Token header. This authentication token will be commonly called
the add-on token throughout the documentation.

There are several ways this token can be retrieved, as well as several
types of tokens that are available. The two primary ways an add-on token
can be retrieved are:

during installation as part of the installed lifecycle when a UI
component is loaded How does the add-on UI integrate with Clockify?# An
add-on can define its UI elements in the manifest by defining UI
components.

UI components are entry points to the UI of the add-on. They are HTML
pages which Clockify loads inside iframes in order to integrate them
into Clockify's UI. There are several types of UI components, each with
its own locations, that can be configured.

How does the add-on UI interact with Clockify?# UI components can
interact with Clockify in several ways:

by calling the Clockify API

by calling the add-on backend, which in turn interacts with the Clockify
API

by listening to or dispatching window events

UI components are loaded and rendered inside iframes. At the time they
are loaded, the components are provided with an authentication token
that they can use in order to communicate with the Clockify API. This
authentication token will also contain a set of claims that can be used
to retrieve information regarding the environment, the workspace and the
user that is currently viewing the UI.

How do add-on settings work?# There are two ways an add-on can display
an interface for its settings:

Using configurable no-code UI Add-on settings can be defined in the
manifest with Clockify taking care of both rendering them to the user
and storing the data. This approach is the fastest way to get started
with building add-ons and supports building customizable settings
screens in a straightforward way. Visit the structured settings section
for more information.

Using a custom settings UI An add-on can be configured to define and
host its own settings screen. This setup can be beneficial if the UI is
complex, if you'd like to store settings in your own infrastructure, or
if the settings need to follow a specific design. The settings UI will
work the same as any other UI component.

How does an add-on work?# After an add-on is installed, it's added to
the workspace and loaded whenever a user loads the Clockify app.

There are several ways in which Clockify interacts with the add-on:

Lifecycle events: Add-on receives events when installed, deleted, if its
settings are updated, or status is changed Webhooks: Add-on receives
webhooks for all the events it has subscribed to on the manifest
Components: Add-on receives requests to render a component whenever a
user navigates to it Components Window Messages: Add-on components can
receive window events after they are loaded An add-on can work in both
interactive (responding to user interactions or events) and
non-interactive (responding to Clockify webhooks or processing server
side jobs) ways.

Can new features be added after an add-on is published?# You can add new
features or improve existing ones after an add-on is published.

However, there are certain changes that require updating the manifest
and/or other data such as the add-on name and the marketplace listing
that are required to go through an approval process.

Changes to the manifest, such as adding or updating components,
lifecycle webhooks or scopes, will only take effect after a new version
of the add-on is approved and published.

Developer Resources# Add-on code examples

Add-on code examples are used to demonstrate how to use add-on's
specific features or functionality. These examples are tested and
functional, therefore you can use them as a reference and build upon
them to create your own custom integrations.

Add-on SDK

Add-on SDK is written in Java and aims to help you with the development
of your add-ons. It contains various modules to help you with the
development, including schema models, validators, helpers, as well as
support for web frameworks.

Add-on web components

Add-on web components are a set of components and CSS styles aimed to
help you develop your UIs, and, at the same time maintain a design style
that is consistent with the CAKE.com style guide. For more information,
visit the Add-on web components documentation.

Next steps# For further information on how add-ons work in practice and
how to develop an add-on you can read our Quick Start Guide.

Next Page Quick Start

ON THIS PAGE Clockify Add-on basics What is the structure of an add-on?
How does add-on hosting infrastructure work? How does the add-on
interact with the Clockify API? How does the add-on UI integrate with
Clockify? How does the add-on UI interact with Clockify? How do add-on
settings work? How does an add-on work? Can new features be added after
an add-on is published? Developer Resources Next steps © 2025 CAKE.com
Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Clockify Add-on Quick Start Guide# Developer account# Before
starting our development process, we will need to create a CAKE.com
developer account. A developer account will allow us to publish, manage
and monetize our add-ons as well as provide us with a testing
environment and workspace where the add-on can be installed and tested
during development. You can learn more about how to get started with the
developer account here.

Let's get started with developing a simple add-on.# In this guide we'll
build a new add-on from scratch and go through the development steps one
by one, but first we have to define what an add-on is.

Add-ons are software components that can be used to extend the
functionalities of CAKE.com products. They are technology-agnostic,
meaning they can be developed using the languages and frameworks of your
choice. For this guide we will be writing our add-on in Java, using the
CAKE.com Addon SDK and UI Kit.

Defining our add-on and its scope# Before starting our development
process, we'll need to define how our add-on will look like and what it
aims to achieve.

For this guide, we will create a simple add-on which will add a new page
to the Clockify's sidebar and will display a statistic of the time
tracked by the current user. As part of the guide we will implement and
host a backend service and also integrate our add-on into Clockify's UI.

What to build# If you're not sure what to build and what needs users
have, you can check our roadmap and get ideas from suggested add-ons or
feedback Forum categories.

Setting up a new project# We'll start our development flow by creating a
new project for our add-on. Let's call it "Time Reports".

We'll create our first file named TimeReportsAddon.java, containing the
following code: package com.cake.marketplace.examples.timereports;

public class TimeReportsAddon { public static void main(String\[\] args)
{ } }

We'll need to set up our Maven dependencies. This step consists of three
parts: Configure and authenticate Github with maven as described in the
following page. Configure our package repository in the pom.xml file by
adding the following snippet: `<repositories>`{=html}
`<repository>`{=html} `<id>`{=html}github`</id>`{=html}
`<url>`{=html}https://maven.pkg.github.com/clockify/addon-java-sdk`</url>`{=html}
`</repository>`{=html} `</repositories>`{=html} Configure our package
dependency in the pom.xml file by adding the following snippet:
`<dependencies>`{=html} `<dependency>`{=html}
`<groupId>`{=html}com.cake.clockify`</groupId>`{=html}
`<artifactId>`{=html}addon-sdk`</artifactId>`{=html}
`<version>`{=html}1.4.0`</version>`{=html} `</dependency>`{=html}
`</dependencies>`{=html} Building our manifest# Now that we have
successfully set up our project and dependencies we can start working on
the add-on itself. First, let's briefly explain what a manifest is.

A manifest is a file which describes an add-on and its functionalities.
Through the manifest you can define how your add-on integrates with
CAKE.com products.

The format of the manifest file depends on the CAKE.com product your
add-on is targeting, but in general will contain information about the
add-on such as its identifier, name, permission scopes and other
definitions that describe how it will interact with the product. You can
read more about the manifest, its syntax and various options on the
manifest section of the documentation.

The Addon SDK provides a simple way to build and host our manifest file
dynamically. Let's start by defining the manifest object, and then we'll
go over the details.

Our main() method will look like this:

public static void main(String\[\] args) throws Exception { var manifest
= ClockifyManifest.v1_3Builder() .key("time-reports-example")
.name("Time Reports") .baseUrl("") .requireFreePlan()
.description("Example add-on that displays time statistics")
.scopes(List.of(ClockifyScope.TIME_ENTRY_READ)) .build(); }

Let's go over the lines step by step.

Builder interface We'll start by using the builder interface to guide us
through the steps needed to construct our manifest. The manifest builder
interface exposes one method for each supported schema version. In this
guide we will use version 1.3 of the manifest schema.

Key The add-on key acts as an identifier for our add-on and must be a
unique value among all add-ons published on the CAKE.com marketplace.

Name The add-on name is also a required field and must match the name
under which the add-on gets published to the CAKE.com marketplace. In
the UI, the name will be displayed on Clockify's add-on settings tab.

Base URL The base URL is the URL that will act as the base location for
our add-on. Other paths will be defined relative to this URL. We're
setting it to an empty value at this point as we do not yet have a
publicly available URL we can use.

Subscription plan The minimal subscription plan is the minimum plan that
Clockify workspaces must have in order to be able to install and use our
add-on. The requireFreePlan() method is a helper which sets the minimum
required plan value to 'FREE'. Other supported values for the plan can
be found on the manifest section.

Description The description field is an optional field, and can be
populated with a short description of the add-on. The text will be
visible on Clockify's add-on settings tab.

Scopes Scopes are optional, but have to be defined if we intend to use
certain features of the Clockify API. For our example we need to request
the TIME_ENTRY_READ scope since we will be making requests to the time
entries endpoint.

Building our UI component# Now that we've built the manifest object, the
next step will be to define and serve our UI component. Let's start by
defining our component first, and then we'll get to the HTML part of the
UI.

var uiComponent = ClockifyComponent.builder() .sidebar()
.allowEveryone() .path("/sidebar-component") .label("Time Reports")
.build();

In this guide we will be defining a single UI component which will be
shown as a separate entry on the sidebar.

Builder Similar to the manifest, we will use a builder interface to
construct our component.

Location Specifies where the entry point where our component will be
located. We've chosen the sidebar location for this example, but there
is a wide range of options that can be chosen depending on your use
case. For more information on the location field, visit the manifest's
components section.

Access Specifies the type of permission the user must have in order to
access our component. Supported values are admins, or everyone. We will
be showing our add-on to every user of the workspace. For more
information on the access field, visit the manifest's components
section.

Path The path is a value relative to the base URL defined in the
manifest. A GET request will be made to this location every time the UI
component is loaded in UI.

UI components are loaded inside iframes and will always be supplied with
an authentication token query parameter, named as auth_token. This token
can be used to identify and authenticate the user who is viewing our
component. For more information on the component's authentication, visit
the authentication section.

Label The label is a text field whose value will be shown on the
component's defined location - the sidebar in our case. You can read
more about the label under the manifest's components section.

Building our UI# To build our UI we will use the CAKE.com Add-on UI kit.
Let's start by creating an empty sidebar-component.html file under our
resources folder.

Import dependencies Let's add the required imports for the Add-on UI
kit. For the purpose of this guide we will also be importing the
date-fns library which will be used to process time values.

```{=html}
<html>
```
```{=html}
<head>
```
`<link
               rel="stylesheet"
               href="https://resources.developer.clockify.me/ui/latest/css/main.min.css"
       />`{=html}
```{=html}
<script src="https://resources.developer.clockify.me/ui/latest/js/main.min.js"></script>
```
```{=html}
<script src="https://cdn.jsdelivr.net/npm/date-fns@4.1.0/cdn.min.js"></script>
```
```{=html}
</head>
```
```{=html}
</html>
```
Define UI elements Now that we've imported the UI kit, our next step is
to create a body tag and then define our UI elements.

```{=html}
<body>
```
```{=html}
<div class="tabs">
```
       <div class="tabs-header">
           <div class="tab-header active" data-tab="tab1">Time Reports</div>
       </div>
       <div class="tabs-content">
           <div class="tab-content tab1 active">
                <p>Your total tracked time is: <span id="tracked-time"></span></p>
           </div>
       </div>
    </div>

```{=html}
</body>
```
Retrieving and decoding the authentication token Our next goal will be
to retrieve our time entries data through Clockify's API and present a
simple overview of the total time tracked. The first thing that we need
to do is to create a script section at the end of our file.

```{=html}
<script>
    
</script>
```
Next, we'll retrieve the authentication token that Clockify provides to
our component and use that token to make a call to the Clockify API.

const token = new
URLSearchParams(window.location.search).get('auth_token');

Clockify's add-ons can be installed on a wide range of environments, be
it on a regional instance or even on development test workspaces.

To ensure that add-ons can work independent of the environment where
they are installed, we have to retrieve all the environment-related
information from the authentication token - which is in fact a JWT. You
can learn more about the claims present in the JWT token on the
following link.

For our component, we're mostly interested in the following three
claims:

backendUrl - the URL of the backend (environment) where our API calls
will be made workspaceId - the ID of the workspace where our add-on was
installed user - the ID of the user that is currently viewing our
component It should be noted that each authentication token that is
supplied to the iframe only references a specific add-on install on a
single workspace.

This example will bypass the verification step and only retrieve the
claims from the token, but it's strongly recommended that production
add-ons perform the proper validations before accepting the token as
valid.

The JWT is signed with RSA256. The public key used to verify the token
can be accessed on the following link.

// note: JWT tokens and their respective claims should always be
verified before being accepted const tokenPayload =
JSON.parse(atob(token.split('.')\[1\])); const backendUrl =
tokenPayload\['backendUrl'\] const workspaceId =
tokenPayload\['workspaceId'\] const userId = tokenPayload\['user'\]

Interacting with the API Now, it's time to make the API call.

While Clockify provides endpoints for generating reports, to keep it
simple we're going to call the time entries endpoint which can be found
here.

Note that we're using the values we retrieved from the claims to
construct the endpoint we're calling.

const oneWeekAgo = dateFns.subWeeks(new Date(), 1)

fetch(`${backendUrl}/v1/workspaces/${workspaceId}/user/${userId}/time-entries?` +
new URLSearchParams({'page-size': 500, 'start':
dateFns.formatISO(oneWeekAgo),}).toString(), { headers:
{"x-addon-token": token}, method: "GET" } ).then(response =\> { if
(response.status !== 200) { console.log("Received status" +
response.status) return }

        let totalDurationSeconds = 0;
        response.json().forEach(entry => {
            const start = entry["timeInterval"]["start"]
            const end = entry["timeInterval"]["end"]

            const durationSeconds = dateFns.differenceInSeconds(dateFns.parseISO(end), dateFns.parseISO(start))
            totalDurationSeconds += durationSeconds;
        });

        const element = document.getElementById("tracked-time");
        if (totalDurationSeconds !== 0) {
            const duration = dateFns.intervalToDuration({ start: 0, end: totalDurationSeconds * 1000 })
            element.textContent = dateFns.formatDuration(duration);
        } else {
            element.textContent = "no time tracked";
        }
        })
    .catch(e => {console.log("Could not retrieve time entries")

})

In the above snippet, we call the Clockify API and retrieve a list of
time entries for our user. Then, we iterate through the entries and sum
up all the durations which will then be used to display the total
duration in a human friendly way.

You will notice we supplied a header named x-addon-token. The token used
to make this API call is the same token that was supplied at the moment
our component was served.

This token has access only over the user that is currently viewing our
UI component inside the Clockify iframe, and unlike the token that is
supplied during the add-on install expires in 30 minutes.

Serving our add-on# Now that our UI is ready, we need to go back to our
Java class and set up the webserver for our add-on.

The Addon SDK provides an embedded web server which we can use to
quickly set up and serve our manifest and the UI.

var addon = new ClockifyAddon(manifest);
addon.registerComponent(uiComponent, (request, response) -\> { var
classLoader = Thread.currentThread().getContextClassLoader(); var
inputStream = classLoader.getResourceAsStream("sidebar-component.html");
inputStream.transferTo(response.getOutputStream());
response.setStatus(HttpServletResponse.SC_OK); });

var servlet = new AddonServlet(addon); var server = new
EmbeddedServer(servlet); server.start(8080);

The manifest will be automatically served under the /manifest path,
while for the UI we will register a handler which will load the HTML
file from the resources and serve it to the response.

The UI component will be served under the path that was previously
configured.

Our add-on is ready to be run, simply run the main() method and access
the add-on manifest on {baseUrl}/manifest to install it on your
development workspace.

Testing our add-on# Developer account# Now that we've got our add-on up
and running we are ready to continue with the next step - trying out the
add-on on the Clockify developer environment.

In order to do so, we first need to create a developer account.

Testing environment# Once the account is set up, the next step is to
access the testing environment for our account. The testing environment
is a pre-populated environment where you can test how your add-on would
work in a real Clockify workspace. Testing environments are unique to
each developer account, and the created workspaces have access to all
Clockify features.

To access the testing environment, follow these steps:

Log in to your developer account Navigate to the Test accounts section
and log in as one of the pre-made users You'll be redirected to the
Clockify test environment From there, go to Workspace settings Select
the Add-ons tab (visible only to users with the administrator role)
Insert the link to your add-on manifest and click Install A toast
message will appear indicating a successful installation. All the
installed add-ons will be listed on this page.

Multiple add-ons can be installed on testing environments as long as the
manifest keys are unique among them.

Testing Environment Add-ons

The following info will be displayed on this page:

Icon (if available, if not -- default image) Name (from manifest) Status
(enabled/disabled) Short description (from manifest) More options
(listed if there are any) Settings (Optional and defined in manifest. If
they exist, you can click on the Settings, and the Add-on settings page
will open.) There are two types of settings:

Clockify settings -- saved on our server and specified in manifest
Settings -- add-on settings that can be done by developers and displayed
as an iFrame on this page) Enable/disable (Installed add-on is
automatically enabled) Webhooks (opens a modal regarding add-on
webhooks) Modal that opens displays webhooks that are sent from Clockify
to this add-on. Uninstall (disabled add-on and removes it from the list)
To maintain a secure environment and prevent potential misuse, your data
will be automatically cleared every month.

Providing a public URL# To be able to install our add-on, we first need
to provide a public URL. We can do so by using a reverse proxy service
such as ngrok. Let's create a free account at ngrok and follow their
guidelines for installing & running app docs.

After you've setup ngrok, run the following command:

ngrok http 8080

This will generate tunnel to your app running on port 8080.

Ngrok CLI

Note the public URL in the output:

https://7ba8-188-246-34-133.eu.ngrok.io

We can now go back to our add-on's code and provide this public URL as
the base URL for our manifest.

Installing our add-on# Once we've opened the add-ons tab on our
workspace, we are ready to install our add-on. With the add-on webserver
running and our public URL configured, we'll paste in the link to our
manifest {base URL}/manifest and click on install.

We can now see that our add-on has been successfully installed. It will
show up on the sidebar like in the screenshot below:

Sidebar Add-on

We can now click on our sidebar entry to access our add-on's UI.

You may notice that our sidebar entry is using a default icon. You can
specify a custom icon by setting the iconPath property on the UI
component.

UI Add-on

You may observe the UI component has been loaded inside an iframe. UI
components are always loaded inside iframes, irrespective of their
location.

Next steps# We've prepared a Development Checklist which will help you
ensure that you've taken care of all the important aspects of the add-on
development flow.

After you've gone through our development checklist, you are ready to
check out our add-on publishing section for a detailed overview of the
add-on publishing flow.

Previous Page Introduction Next Page Manifest

ON THIS PAGE Developer account Let's get started with developing a
simple add-on. Defining our add-on and its scope What to build Setting
up a new project Building our manifest Building our UI component
Building our UI Serving our add-on Testing our add-on Developer account
Testing environment Providing a public URL Installing our add-on Next
steps © 2025 CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Manifest# Definition# A manifest is a file which describes an
add-on and its functionalities. Through the manifest you can define how
your add-on integrates with Clockify.

Schema# While there are several manifest schema versions available, you
should always aim to support and use the latest version of the manifest
schema. At the time of writing, the latest manifest schema version is
version 1.3. This section will describe version 1.3 of the manifest
schema.

The manifest schema is defined in the json schema format and can be
retrieved at any time from the Clockify developer API by calling the
following endpoint:

GET https://developer.clockify.me/api/addons/manifest-schema You can use
the schema above to validate your manifest file and explore the
different available options.

Versions

In specific circumstances (such as for compatibility reasons), you might
want to use a previous version of the schema. To do so, you have to
explicitly define the schemaVersion attribute on the manifest.

To access a specific version of the manifest schema, you can call the
following endpoint with the version parameter:

GET https://developer.clockify.me/api/addons/manifest-schema?version=1.3
Top-level properties# Property Required Updateable Description
schemaVersion no yes The version of the manifest schema the add-on is
targeting. If left undefined, it will default to the latest version. key
yes no The add-on key acts as an identifier for our add-on and must be a
unique value among all add-ons published on the CAKE.com marketplace
name yes yes The name of the add-on. It will be displayed on Clockify's
add-on settings tab baseUrl yes yes The base URL is the URL that will
act as the base location for our add-on. Other paths will be defined
relative to this URL. The value can be updated to interact with
different environments of the add-on such as local or testing
deployments. description no yes A brief description of the add-on. The
text will be visible on Clockify's add-on settings tab. iconPath no yes
Path to an image that will act as the icon for the add-on. The path must
be relative to the baseUrl defined above. lifecycle no yes Lifecycles
are messages that are automatically sent whenever an event specific to
the add-on installation is triggered. Examples include events sent when
an add-on is installed or uninstalled. webhooks no yes Webhooks are
messages that are automatically sent whenever a specific event is
triggered on Clockify. You can read more about the events you can
subscribe to on the webhooks section. components no yes Components are
UI elements that integrate you add-on's UI into Clockify. settings no
yes The settings value can either be a structured object according to
the schema of the structured settings or a path value pointing to a
custom settings UI. Add-on settings can be accessed through the add-on
menus on the add-ons tab. minimalSubscriptionPlan yes no Minimal
subscription plan that is required for the add-on to work. This plan is
used when checking if user is able to install the add-on on their
workspace. scopes no yes Scopes represent what the add-on is permitted
to do, and the data it's allowed to access. Components# Property
Required Description type yes The type of the component. You can read
more about the different types on the components section. label yes
Custom label for component. The value will be displayed on the UI
depending on the component type. path yes Path of the component relative
to the baseUrl. iconPath no Path of the component's icon relative to the
baseUrl. accessLevel yes The access level a users must have in order to
be able to view the component. width no The width of the component.
height no The height of the component. Example

"components": \[ { "type": "sidebar", "label":
"${sidebarLabel}",  "accessLevel": "ADMINS",  "path": "${sidebarPath}",
"options": { "option 1": "option 1 value", "option 2": "option 2 value"
}, "iconPath": "/trt"\] Lifecycle# Lifecycles contain events add-on
receives when installed, deleted, or when its settings are updated.

Property Required Description path yes Path relative to the baseUrl type
yes The lifecycle event you want to subscribe to. Possible values:
INSTALLED, SETTINGS_UPDATED, STATUS_CHANGED, DELETED Example

"lifecycle": \[ { "path": "/postinstall", "type": "INSTALLED" }, {
"path": "/uninstalled", "type": "DELETED" }\] Webhooks# Property
Required Description path yes Path of the endpoint relative to the
baseUrl. event yes The even that is being subscribed to. Possible values
can be found on the webhooks section. Example

"webhooks": \[ { "event": "NEW_TIME_ENTRY", "path": "/new-time-entry" },
{ "event": "NEW_CLIENT", "path": "/new-client" }\] You can have a
maximum of 10 webhooks per add-on.

Settings# Add-on settings allow the add-on to present the user with a
configuration screen where add-on specific settings can be configured.
If defined, settings will be shown under the menu dropdown of the add-on
on the add-ons tab.

Types# There are two types of settings an add-on can have:

structured settings - you can configure the structure of the settings
according to the settings schema. Clockify will take care of the
persisting the settings. custom UI - you can configure a custom UI by
supplying a string value as the value for the settings field. The value
will be the path where the UI will be served. The custom settings UI
will work the same way as UI components do and will be loaded inside an
iframe. If following this approach, you will be responsible for handling
and storing your add-on's data. Subscription plan# Subscription plans
represent the plans a workspace can be subscribed to on Clockify and the
features that are available to said workspace.

Below is the list of plans that are currently supported:

FREE BASIC STANDARD PRO ENTERPRISE You can read more about the different
subscription plans, and the features that are available to each plan, on
the following link.

Scopes# The scopes represent the permissions of an add-on and constrain
the APIs and data that the add-on can access when interacting with the
Clockify API. A scope consists of a resource and an action. Actions can
be READ or WRITE.

Requesting the WRITE permission for a resource does not automatically
grant the add-on READ permission.

Attempting to call endpoints without declaring the appropriate scopes on
the manifest will result in the request failing with a status of HTTP
403 FORBIDDEN.

It should be noted that even if a specific feature is available on the
workspace plan and the relevant scopes are requested, the feature still
needs to be enabled in the workspace settings in order for the APIs for
that feature to work.

Below is the list of scopes that an add-on can declare in its manifest:

CLIENT_READ CLIENT_WRITE PROJECT_READ PROJECT_WRITE TAG_READ TAG_WRITE
TASK_READ TASK_WRITE, TIME_ENTRY_READ TIME_ENTRY_WRITE EXPENSE_READ
EXPENSE_WRITE INVOICE_READ INVOICE_WRITE USER_READ USER_WRITE GROUP_READ
GROUP_WRITE WORKSPACE_READ WORKSPACE_WRITE CUSTOM_FIELDS_READ
CUSTOM_FIELDS_WRITE APPROVAL_READ APPROVAL_WRITE SCHEDULING_READ
SCHEDULING_WRITE REPORTS_READ REPORTS_WRITE TIME_OFF_READ TIME_OFF_WRITE
Previous Page Quick Start Next Page Lifecycle

ON THIS PAGE Definition Schema Top-level properties Components Lifecycle
Webhooks Settings Types Subscription plan Scopes © 2025 CAKE.com
Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Lifecycle# Definition# The lifecycle of an add-on consists of
all the steps beginning from when it's installed until when the add-on
gets uninstalled.

Throughout its lifecycle whe add-on may be in one of the two states:

active - it's loaded on the Clockify UI, receives events and can
interact with the Clockify API inactive - it's not loaded and cannot
interact with Clockify, but it's still installed and all the user data
are still kept A general lifecycle of an add-on includes the following
events:

Installed Status changed Settings updated Deleted Types# Installed#
Add-on is installed on a workspace.

To receive this event, the add-on must declare the INSTALLED lifecycle
hook as part of its lifecycles. During installation, a lifecycle event
is triggered and the add-on is provided with the installation context
and a set of tokens.

It is important to note that this payload will only be supplied once for
each add-on installation.

We recommend persisting the installation payload in your database if
your add-on meets, or plans to meet in the future, the use cases where
an installation token may be needed.

The data included in the installation payload, including the API URLs
and tokens, are dependent on the environment and the region where the
add-on is installed. If you intend to interact with the Clockify APIs,
you must always use the appropriate URLs for the specific environment.

Example of a payload that is sent as part of the INSTALLED event:

Request Headers Content-Type : application/json X-Addon-Lifecycle-Token
: {{token}}

{ "addonId": "62ddf9b201f42e74228efa3c", "authToken": "{{token}}",
"workspaceId": "60332d61ff30282b1f23e624", "asUser":
"60348d63df70d82b7183e635", "apiUrl": "{{apiUrl}}", "addonUserId":
"1a2b3c4d5e6f7g8h9i0j1k2l", "webhooks": \[{ "path":
"https://example.com/webhook" "webhookType": "ADDON" "authToken":
"{{token}}" }\], }

The authToken is an API token that can be used to make authenticated
requests to the Clockify API. For more information, read the
Authentication and authorization sectuib.

Status changed# After installation, the add-on is automatically enabled
and becomes active.

There are cases where the user may choose to deactivate an add-on
instead of uninstalling it, for instance if they do not wish to use the
add-on at the present but still want to preserve their settings and
configurations.

To receive this event, the add-on must declare the STATUS_CHANGED
lifecycle hook as part of its lifecycles.

Example of a payload that is sent as part of the STATUS_CHANGED event:

Request Headers Content-Type : application/json X-Addon-Lifecycle-Token
: {{token}}

{ "addonId": "62ddf9b201f42e74228efa3c", "workspaceId":
"60332d61ff30282b1f23e624", "status": "INACTIVE" }

The status values can be either ACTIVE or INACTIVE

Settings updated# An add-on can have its settings structure defined
inside the manifest. In these cases, the add-on can subscribe to the
SETTINGS_UPDATED lifecycle hook to be notified anytime one of its users
updates the settings for the add-on.

Example of a payload that is sent as part of the SETTINGS_UPDATED event:

Request Headers Content-Type : application/json X-Addon-Lifecycle-Token
: {{token}}

{ "workspaceId": "60332d61ff30282b1f23e624", "addonId":
"62ddf9b201f42e74228efa3c", "settings": \[ { "id": "txt-setting",
"name": "Txt setting", "value": "Some text" }, { "id": "link-setting",
"name": "Link setting", "value": "https://clockify.me" }, { "id":
"number-setting", "name": "Number setting", "value": 5 }, { "id":
"checkbox-setting", "name": "Checkbox setting", "value": true }, { "id":
"dropdown-single-setting", "name": "Dropdown single setting", "value":
"option 1" }, { "id": "dropdown-multiple-setting", "name": "Dropdown
multiple setting", "value": \[ "option 1", "option 2" \] } \] }

Deleted# When an add-on is deleted from a workspace, a lifecycle event
is triggered and the add-on is provided with the context for the
installation that is being uninstalled. All the tokens that are provided
to the add-on become invalid and from that moment on, the add-on can no
longer interact with the Clockify API on behalf of the workspace user.

Example of a payload that is sent as part of the DELETED event:

DELETED

Request Headers Content-Type : application/json X-Addon-Lifecycle-Token
: {{token}}

{ "addonId": "62ddf9b201f42e74228efa3c", "workspaceId":
"60332d61ff30282b1f23e624", "asUser": "60348d63df70d82b7183e635" }

Previous Page Manifest Next Page UI Components

ON THIS PAGE Definition Types Installed Status changed Settings updated
Deleted © 2025 CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment UI Components# The UI components are pages which are used to
integrate your add-ons UI with Clockify. They serve as an entry point to
the user's interaction with your add-on.

UI components are served as HTML pages and loaded inside iframes when
redered on the Clockify site. At the time of loading, components will be
provided with the relevant context they need in order to function such
as an authentication token as well as information the current location
on the app and information about the user.

Entrypoints to your components can be added to a variety of available
locations on the Clockify site.

Definition# UI components must be defined in the add-on manifest. They
contain information about the location, label, icon and other
component-specific attributes as detailed on the components section of
the manifest. Components can be added, modified or removed in subsequent
versions of you add-on. Changes to an add-on components will not take
effect until the new version of the add-on is approved and published.

Settings UI# The settings UI is also a UI component, with the only
difference being that it's not defined as part of the components but
rather as part of the settings field of the manifest.

Interacting with the API# UI components can interact with the Clockify
API in two ways:

by calling the API directly All components are provided with an
authentication token in the form of a query parameter. This token has
the same permissions and access as the user who is currently viewing the
UI. This token, which in case it's provided to UI components is called
the user token, can be used to make authenticated requests to the
Clockify API.

by interacting with an add-on backend service In this case it's up to
the developer to implement the relevant authentication mechanisms and
APIs. The add-on will be provided with an installation token in case the
installed lifecycle has been configured, although you may also choose to
forward the user token to your backend service as part of the requests.

Types# There are three types of components in Clockify, each having its
own location where the component will be shown.

Sidebar# Sidebar location

A sidebar entry is an entry that is added to the add-ons section of the
Clockify sidebar, located on the left side of the page. New entries
always default to the add-ons section, although they can be manually
moved according to the user's preferences. Add-on can add new element in
a sidebar automatically, or if user enables it through Settings by
clicking Show more and Add-ons.

Now, you can specify sidebar's properties:

Added if at least one enabled add-on has "Sidebar" component specified
in manifest Not removed even if empty Users can put other elements in it
(from other sections) New add-on sidebar elements are always added to
the Add-on section Element can be moved from the Add-on to other
sections and is removed from the Add-on section if it is uninstalled or
disabled in the optional add-on's settings.

Widget# A widget is a small icon which is displayed at the bottom-right
section of the page. It serves an entrypoint to UI components that are
defined with the WIDGET type.

Widget location

The widget icon will serve as an entrypoint to one or more UI components
according to the rules below:

Widget icon is displayed if at least one add-on with widget component is
installed and enabled at the bottom right corner of the page.

Widget list is displayed if more than one add-on with widget component
is installed and enabled at the bottom right corner of the page.

Widget is not visible if there are no installed add-ons that have widget
as a component in the manifest, or if all add-ons with widget component
are disabled.

Tabs# A tab is a location that can be defined for components which
extend the functionality of existing pages with tabs. Add-on tabs will
be added after the default tabs of the pages that support them.

Tab location

Add-on can add new element in tab automatically, or if user enables it
through Settings. Add-on tabs are added after the existing tabs and are
sorted by the date when they where added.

Tabs can be added to the following pages:

Time off Schedule Approvals Reports Activity Team Project Settings UI#
The Settings UI is technically also a UI location, although it must be
configured on the settings field rather than as a component. The UI can
be accessed through the add-on options dropdown on the add-ons tab.

Settings location

Access# A component can be configured to be visible to everyone, or only
to users with the admin role.

Previous Page Lifecycle Next Page Webhooks

ON THIS PAGE Definition Settings UI Interacting with the API Types
Sidebar Widget Tabs Settings UI Access © 2025 CAKE.com Inc. All Rights
Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Webhooks# Definition# Webhooks are a way for your add-on to
respond to events and triggers in real-time without the user directly
interacting with the add-on UI itself. They can be used to integrate
your add-on with Clockify in a seamless way.

Webhook messages are automatically sent by Clockify whenever an event
that the add-on has subscribed to is triggered. Clockify provides a
variety of Webhook Event types that an add-on can subscribe to according
to its needs.

Types# There are different types of webhooks that your add-on can
subscribe to. The webhooks that are available for your add-on depend on
the specific version of the manifest schema that you choose.

Generally, the following webhooks are available to add-ons:

NEW_PROJECT PROJECT_UPDATED PROJECT_DELETED NEW_TASK TASK_UPDATED
TASK_DELETED NEW_CLIENT CLIENT_UPDATED CLIENT_DELETED NEW_TAG
TAG_UPDATED TAG_DELETED NEW_TIMER_STARTED TIMER_STOPPED
TIME_ENTRY_UPDATED TIME_ENTRY_DELETED NEW_TIME_ENTRY NEW_INVOICE
INVOICE_UPDATED USER_JOINED_WORKSPACE USER_DELETED_FROM_WORKSPACE
USER_DEACTIVATED_ON_WORKSPACE USER_ACTIVATED_ON_WORKSPACE
USER_EMAIL_CHANGED USER_UPDATED NEW_APPROVAL_REQUEST
APPROVAL_REQUEST_STATUS_UPDATED TIME_OFF_REQUESTED
TIME_OFF_REQUEST_APPROVED TIME_OFF_REQUEST_REJECTED
TIME_OFF_REQUEST_WITHDRAWN BALANCE_UPDATED USER_GROUP_CREATED
USER_GROUP_UPDATED USER_GROUP_DELETED EXPENSE_CREATED EXPENSE_UPDATED
EXPENSE_DELETED ASSIGNMENT_CREATED ASSIGNMENT_UPDATED ASSIGNMENT_DELETED
ASSIGNMENT_PUBLISHED You can test and visualize how the webhooks work
and their respective payloads by triggering and listening for the events
on your development environment.

Requests# Webhook requests are POST requests that are sent to notify the
add-on of events it has subscribed to. Each specific event will contain
its specific payload as well as an accompanying signature that can be
used to verify the request. After installing an add-on, you can view a
list of all the registered webhooks by navigating to the add-ons tab and
clicking on the webhooks option.

Webhooks Dropdown

A list of all the registered webhooks along with their endpoints will be
displayed.

Webhooks List

You can access a webhook's logs by clicking on the webhook event. The
logs will contain information such as the timestamp when the request was
made, the HTTP status as well as the request and response bodies.

Webhooks Logs

Webhook logs are deleted after 7 days.

Signature# Each webhook that is dispatched by Clockify will contain a
signature that can be used to verify its authenticity. A typical webhook
request will contain the following request headers:

clockify-signature - this represents the token that is signed on behalf
of a single webhook type for a single add-on installation
clockify-webhook-event-type - this represents the event that triggered
the webhook, must be one of the webhook values above Webhook token# The
webhook token supplied as part of the clockify-signature headers does
not expire. It contains the following claims that can be used to verify
its authenticity and determine its context:

"iss": "clockify", "sub": "{add-on key}", "type": "addon",
"workspaceId": "{workspace id}", "addonId": "{add-on id}" iss - the
issuer of a JWT will always be clockify sub - the sub must be the same
as the add-on key type - the type of a JWT will always be addon
workspaceId - the ID where the add-on is installed and where the event
was triggered addonId - the ID of the add-on installation on the
workspace Authenticity# There are a couple of precautions that we must
take to verify a webhook's authenticity and prevent request spoofing.

Verify the JWT The JWT token must be verified and the issuer and the sub
claims must match the expected values for our add-on. To learn more
about the tokens, visit the Authentication & Authorization section.

Assert the webhook type is the one you expect You must assert that the
webhook types and the payloads supplied with the request match the
webhook types that you expect for each endpoint.

Compare webhook tokens When an add-on which has defined an installed
lifecycle gets installed on a workspace, an installation payload is
provided along with the installed event. If the add-on has defined
webhooks in its manifest, the payload will contain information regarding
registered webhooks as well as the webhook token for each of them.

{ ... "webhooks": \[ { "authToken": "{JWT for the webhook}", "path":
"{path defined in the manifest}", "webhookType": "ADDON" } \], ... } It
is recommended that add-ons retrieve and store the authToken for each
registered webhook, so that it can later be used to verify the
authenticity of the requests.

The webhook token does not expire, and the same token registered for a
particular webhook will be sent as part of the clockify-signature header
for every webhook event of that type that is triggered on the workspace.

Previous Page UI Components Next Page Settings

ON THIS PAGE Definition Types Requests Signature Webhook token
Authenticity © 2025 CAKE.com Inc. All Rights Reserved.

Terms Privacy logo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Structured settings# Definition# Settings UI example

Structured settings are a way for you to easily and declaratively create
a UI for the settings of your add-on.

Structured settings are a great way to get started with building the
settings for your add-on while ensuring they integrate flawlessly with
Clockify's UI and follow the same styling guidelines as Clockify does.
The structure of the settings is flexible and capable of building and
supporting complex settings structures and UIs.

Interactions# UI Clockify handles building and rendering the UI for the
add-on settings, as well as persisting the settings whenever they are
updated.

APIs Clockify exposes a set of APIs that add-ons can use to retrieve and
update their settings for a particular workspace.

Lifecycle Add-ons can subscribe to the settings updated lifecycle event
to be notified whenever the settings are updated.

Properties# This section will briefly describe the structure of the
settings definition. More details and possible values for a particular
field can be found under the definitions list of the manifest schema for
the manifest version that you are targeting.

Settings are organized by nesting tabs and groups. Values and types of
the settings must be compatible.

Tabs# Tabs are at the top level of hierarchy when defining settings.

Tabs cannot be nested in other tabs or groups, and they need to have at
least one type of settings defined. If the groups property is defined,
it needs to have at least one settings property defined in it. For more
information on groups, check out the Groups section.

Property Required Description id yes Tab identifier name yes Tab name,
will be displayed on the UI header no Text shown on the settings header
settings no List of settings contained in the tab groups no List of
groups contained in the tab Example

"settings": { "tabs": \[ { "id": "Tab id", "name": "Tab one title",
"header": { "title": "Title text" }, "groups": \[ { "id": "Group id",
"title": "Group one title", "description": "Group description",
"header": { "title": "Header title" }, "settings": \[ { "id": "Setting
id", "name": "Default setting", "description": "Description of default
setting", "placeholder": "Default setting here...", "type": "TXT",
"value": "Value of default setting", "required": true, "copyable": true,
"readOnly": false, "accessLevel": "ADMINS" } \] } \], "settings": \[ {
"id": "Tab setting", "name": "Tab setting", "type": "TXT", "value":
"Some value", "required": true, "accessLevel": "EVERYONE" } \] }\]} \]
Groups# Groups are a way to link related settings. Group can be part of
tabs, and one tab can contain multiple groups.

Property Required Description id yes Group identifier title yes Group
title, will be displayed on the UI description no Brief description the
settings group header no Text shown on the group header settings yes
List of settings contained in the group Example

"groups": \[ { "id": "Group id", "title": "Group one title",
"description": "Group description", "header": { "title": "Header title"
}, "settings": \[ { "id": "Setting id", "name": "Default setting",
"description": "Description of default setting", "placeholder": "Default
setting here...", "type": "TXT", "value": "Value of default setting",
"required": true, "copyable": true, "readOnly": false, "accessLevel":
"ADMINS" } \] }\] Settings# This is the actual settings object which
defines the individual setting elements.

Property Required Description id yes Unique identifier for the settings
property name yes Property name, will be displayed on the UI description
no Brief description of the property placeholder no Placeholder that
will be displayed if the value empty type yes Settings' type. Each type
is displayed differently on the UI key no Serves as the key for the
settings when retrieved as key value pairs value yes Settings' value. It
must match with the type of settings defined in the type property
allowedValues no\* List of allowed values for settings of the dropdown
type. \*Required if type of settings is DROPDOWN_SINGLE or
DROPDOWN_MULTIPLE required no Defines if the setting is required
copyable no Defines if a 'Copy' button will be added next to the setting
value on the UI readOnly no Defines if the setting value is read-only
accessLevel yes Defines the access level a user must have in order to
access this setting Example

"settings": \[ { "id": "Setting id", "name": "Default setting",
"description": "Description of default setting", "placeholder": "Default
setting here...", "type": "TXT", "value": "Value of default setting",
"required": true, "copyable": true, "readOnly": false, "accessLevel":
"ADMINS" }\] Endpoints# Clockify exposes the following API endpoints
which can be used to interact with the stored settings of an add-on:

The requests to these APIs must be authenticated.

Retrieving Settings

Headers: X-Addon-Token: {token}

GET /addon/workspaces/{workspaceId}/settings The endpoint requires the
following parameters:

workspaceId -- ID of workspace where the add-on is installed, can be
retrieved from the claims present in the authentication token The
settings are specific to an add-on installation on a given workspace.

Sample Response:

{ "tabs": \[ { "name": "Tab one title", "id": "tab one id", "groups": \[
{ "id": "group nested in tab", "title": "Group title", "description":
"group description", "header": { "title": "Header title" }, "settings":
\[ { "id": "Setting id", "name": "Default setting", "description":
"Description of default setting", "placeholder": "Default setting
here...", "type": "TXT", "value": "Value of default setting",
"required": true, "copyable": true, "readOnly": false, "accessLevel":
"ADMINS"\] } \], "header": { "title": "title header 2" }, "settings": \[
{ "id": "Setting id 2", "name": "Default setting", "description":
"Description of default setting", "placeholder": "Default setting
here...", "type": "TXT", "value": "Value of default setting 2",
"required": true, "copyable": true, "readOnly": false, "accessLevel":
"ADMINS" } \] } \] } Updating settings

The following endpoint can be used to update one or more add-on settings
by providing the unique settings IDs.

Headers: X-Addon-Token: {token}

PATCH /addon/workspaces/{workspaceId}/settings

\[ {\
"id": "settingId", "value": "New value of setting" }\] Previous Page
Webhooks Next Page Developer Account

ON THIS PAGE Definition Interactions Properties Tabs Groups Settings
Endpoints © 2025 CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Getting Started# Creating an account# Although you can start
building your add-on prior to creating a developer account, you'll need
the account to make the first add-on version and start the add-on
lifecycle, from creating to publishing. Go to the CAKE.com developer
page, create account and make new and exciting products with us.

How to create developer account?# In order to create an add-on version,
or submit any piece of code for a review, you need to have a CAKE.com
Developer account.

To create an account:

Go to the Developer page Choose Sign up Enter email address in the box
Click Sign up Verify your email by clicking on the verification link you
received on your email address.

Verification link expires after 24 hours.

After your email is verified, you'll jump to the modal prompting you to
create an account.

Signup Modal

Enter the following:

Email Name Password Accept our Terms of Use and click Create account to
complete the process.

Use your account to create and edit your vendor profile, create and
manage your add-ons and access your testing environment.

Previous Page Settings Next Page Authentication and Authorization

ON THIS PAGE Creating an account How to create developer account? © 2025
CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Authentication & authorization# Basics# In order to build
add-ons for the Clockify app, CAKE.com Marketplace API needs to interact
with the Clockify API. For an add-on to have access to the Clockify API,
every request needs to have an X-Addon-Token header with a valid add-on
token.

Tokens and API keys that are provided to add-ons by Clockify are
collectively called add-on tokens. Your add-on tokens along with your
API keys should be kept secret. There are several types of add-on tokens
that an add-on can receive, depending on the context. Each specific
token type has its own use cases, as described below.

Rate limits# Requests to the Clockify API are rate limited. Please note
that the limit is subject to change in the future. You can read more
about rate limits on the Clockify documentation.

If the limit is exceeded, a 'Too many requests' error will be returned.

Tokens# Installation token# The installation token is supplied as part
of the installation payload if the add-on has defined an installed
lifecycle in its manifest. For more information about lifecycle requests
sent to add-ons, check out the add-on lifecycle section.

This token is unique and is specific for each add-on installation on a
given workspace. Each time an add-on gets reinstalled a new payload will
be provided. You can obtain this token by reading authToken property of
installed lifecycle hook payload. This token has admin privileges in the
workspace and does not expire.

You must ensure the token value is kept a secret and is not leaked
externally as it has full access over the workspace.

If the add-on is uninstalled from the workspace, the installation token
will no longer be valid.

Example usages

Installation tokens are intended to be used by the add-on's backend.
They can be used in cases where full access over the workspace is
required, long-running operations, reporting etc. Installation tokens
can also be exchanged for user tokens for fine-grained access over a
specific user.

Exchanging for a user token

There may be cases where you might want to use a user token rather than
an installation token, for instance when interacting with a user's
profile.

To do so, Clockify exposes the following endpoint:

Request Headers: Content-Type: application/json X-Addon-Token:
{installation token}

Request Endpoint: POST {backendUrl}/addon/user/{userId}/token

The response body will be a string which will be the user token for the
user specified by the ID. The generated user token will work the same
way as the user tokens that are supplied to iframes.

User token# The user token is supplied as a query parameter whenever a
UI component or a custom settings UI is loaded into Clockify. UI
components are loaded and rendered inside an iframe and the loaded URL
will always contain a query parameter named auth_token. This token is
unique to each user of the add-on on a given workspace.

The user token acts on behalf of a single user, the user that is
currently viewing the UI component, and has a lifespan of 30 minutes.
After a token expires, a new one can be requested by dispatching the
appropriate window event.

As the token acts on behalf of a workspace user, it will have the same
access and permissions as the user does.

Differently from the installation token, the user token will also
contain claims pertaining to the user such as role, language, theme etc.

Example usages

User tokens can be used on both the add-on frontend and the add-on
backend.

On the frontend, through UI components, it can be used to interact and
make requests to the Clockify API. All requests made to the Clockify API
will be on behalf of the current user and will share the same
rate-limiting quota.

On the backend, the user token can either be retrieved through the
frontend or it can be retrieved by exchanging the installation token for
a user token.

Webhook signature# A webhook signature is a type of token that is
provided as a signature to verify the authenticity of webhook requests.

Unlike the installation or user tokens, it cannot be used to
authenticate and interact with the Clockify API. Its purpose is strictly
to be used for request verification.

This token contains a reduced set of claims which can be used to
identify the workspace and the add-on installation for which the event
was triggered. Some of the available claims for this type of token are:

sub workspaceId addonId You can read more about the above claims on the
section below.

Claims# To provide more context on the environment where the add-on has
been installed, both the installation and user tokens will contain a set
of claims that can be used to determine the location of the Clockify API
endpoints or retrieve more information about the installation.

The following set of claims is present in both installation and user
token types:

"backendUrl": "https://api.clockify.me/api", "reportsUrl":
"https://reports.api.clockify.me", "locationsUrl":
"https://locations.api.clockify.me", "screenshotsUrl":
"https://screenshots.api.clockify.me",

"sub": "{add-on key}", "workspaceId": "{workspace id}", "user": "{user
id}", "addonId": "{add-on id}", URL claims - the location of the API
endpoints for the environment where the add-on is installed

sub - the sub field will be the same as the key that is defined in the
add-on manifest. The sub is used to verify that the token is signed on
behalf of your add-on.

workspaceId - the ID of the workspace where the add-on is installed

user - the ID of the workspace owner (for installation tokens), or the
ID of the user who is currently logged in and viewing a UI component
(for user tokens)

addonId - the ID of the add-on installation on the workspace

The following set of claims is present only in the user token type:

"language": "EN", "theme": "DEFAULT", "workspaceRole": "OWNER",
language - the language settings for the current user. You may use this
to support multiple languages and localized content for you add-on.

theme - the theme settings for the current user. It is strongly
recommended that your add-on uses the same theme colors the user has
configured on Clockify.

workspaceRole - the role of the current user on this workspace. Learn
more about roles on the Clockify API docs

Token verification# All tokens signed by Clockify are JWT tokens which
are signed with the RSA256 algorithm

Tokens that Clockify may sign include:

installation token user token webhook signature lifecycle signature The
following is a checklist of the steps that need to be taken to verify
that an add-on token is valid:

Verify signature Before accepting the token or any of its claims as
valid, the signature should first be verified. To verify the token's
signature, you have to use the following X509 public key which is
provided below in PEM format:

-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAubktufFNO/op+E5WBWL6
/Y9QRZGSGGCsV00FmPRl5A0mSfQu3yq2Yaq47IlN0zgFy9IUG8/JJfwiehsmbrKa
49t/xSkpG1u9w1GUyY0g4eKDUwofHKAt3IPw0St4qsWLK9mO+koUo56CGQOEpTui
5bMfmefVBBfShXTaZOtXPB349FdzSuYlU/5o3L12zVWMutNhiJCKyGfsuu2uXa9+
6uQnZBw1wO3/QEci7i4TbC+ZXqW1rCcbogSMORqHAP6qSAcTFRmrjFAEsOWiUUhZ
rLDg2QJ8VTDghFnUhYklNTJlGgfo80qEWe1NLIwvZj0h3bWRfrqZHsD/Yjh0duk6
yQIDAQAB -----END PUBLIC KEY----- Verify token expiration The token
expiration should be checked and expired tokens should be rejected.

Verify token claims The following claims must always match these values
to verify that the token is being used for its intended purpose:

iss=clockify type=addon The iss claim denotes that the issuer is
Clockify and the type claim denotes that the intended usage for this
token is to be used by add-ons.

The sub claim must also be verified to match the expected value, which
must always be the key defined in the add-on manifest.

sub={add-on key} Additional verification# Webhook signatures require an
extra step to verify that the provided signature is the correct one for
the expected event.

Previous Page Developer Account Next Page Environment and Regions

ON THIS PAGE Basics Rate limits Tokens Installation token User token
Webhook signature Claims Token verification Additional verification ©
2025 CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Environments# Add-ons should be able to be installed on every
Clockify workspace, irrespective of environment where the workspace is
located.

To ensure that add-ons will work on all environments, you should avoid
making use of hardcoded values for API endpoints and UI locations and
instead always retrieve environment-specific values from the token
claims.

Different environments where a workspace can be located are:

regions Clockify workspaces can be located in each of the available
regions. If a workspace is located on a region, the entirety of the data
for that workspace is also located in that region. Each region exposes
its own API locations.

subdomains A Clockify workspace can also be located on a subdomain.
Workspaces that are located on subdomains have custom UI locations. A
workspace that is located on a subdomain can also be located on a
specific region.

development environments When testing your add-on, you may notice that
the environment on which you have installed the add-on is a completely
separate one. The development environments use their own separate
locations for all API endpoints.

Ensuring add-ons will work irrespective of the environment# To ensure
that add-ons can work independent of the environment where they are
installed, we have to retrieve all the environment-related information
from the add-on authentication token - which is in fact a JWT.

The following claims can be used to determine the locations where API
calls must be made for a specific workspace:

"backendUrl": "https://api.clockify.me/api", "reportsUrl":
"https://reports.api.clockify.me", "locationsUrl":
"https://locations.api.clockify.me", "screenshotsUrl":
"https://screenshots.api.clockify.me", The above claims represent the
locations of the backend, reports, locations and screenshots API
services.

Retrieving information related to the add-on installation# The add-on
token contains other claims which may be used to identify the add-on
installation and retrieve other useful information such as the workspace
where the add-on is installed, or the user that is currently logged in.

The following claims can be used to retrieve more info related to the
add-on installation:

"sub": "{add-on key}", "workspaceId": "{workspace id}", "user": "{user
id}", "addonId": "{add-on id}", sub - the sub field will be the same as
the key that is defined in the add-on manifest. The sub is used to
verify that the token is signed on behalf of your add-on. workspaceId -
the ID of the workspace where the add-on is installed user - the ID of
the workspace owner (for installation tokens), or the ID of the user who
is currently logged in and viewing a UI component (for user tokens)
addonId - the ID of the add-on installation on the workspace The above
claims are available for both installation and user tokens.

Retrieving information related to the add-on user# Apart from the claims
which provide information related to the environment where the add-on is
installed, there are also claims which provide information related to
the user who is interacting with your add-on.

The following claims can be used to retrieve more info related to the
user who is interacting with the add-on UI components:

"language": "EN", "theme": "DEFAULT", "workspaceRole": "OWNER",
language - the language settings for the current user. You may use this
to support multiple languages and localized content for you add-on.
theme - the theme settings for the current user. It is strongly
recommended that your add-on uses the same theme colors the user has
configured on Clockify. workspaceRole - the role of the current user on
this workspace. Learn more about roles on the Clockify API docs The
above claims are only available for user tokens.

Previous Page Authentication and Authorization Next Page Window Events

ON THIS PAGE Ensuring add-ons will work irrespective of the environment
Retrieving information related to the add-on installation Retrieving
information related to the add-on user © 2025 CAKE.com Inc. All Rights
Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Window messages# Clockify uses the window message API in
order to allow add-on developers receive messages about specific events
and react accordingly.

Clockify supports two-way event communications, where the add-on can
subscribe to specific events as well as dispatch events that should
trigger actions on the Clockify site.

Event subscription# Below is a sample snippet showing how to register a
listener for an event:

handleWindowMessage = (message) =\> { console.log(message.data.title) }

window.addEventListener("message", (event) =\>
handleWindowMessage(event))

Events

Events will contain the following fields:

message.data.title message.data.body The title field will be the name of
the event. The body field will be an optional payload which depends on
the event type.

Current events that can be listened for are:

URL_CHANGED message.data.body={the URL} TIME_ENTRY_STARTED
TIME_ENTRY_CREATED TIME_ENTRY_STOPPED TIME_ENTRY_DELETED
TIME_ENTRY_UPDATED TIME_TRACKING_SETTINGS_UPDATED
WORKSPACE_SETTINGS_UPDATED PROFILE_UPDATED USER_SETTINGS_UPDATED The
above events are not final and are subject to change in the future.

Event dispatch# In addition to listening for events that Clockify
dispatches, the add-on can also interact with Clockify by triggering its
own events.

Current events that can be dispatched from the add-on are:

refreshAddonToken - asks Clockify to refresh the add-on token for the
user that is currently viewing the UI component. Learn more about tokens
and their contexts on the Authentication & Authorization section.
preview - if dispatched from an add-on, it will ask Clockify to open a
modal with the add-on's marketplace listing. navigate - asks Clockify to
navigate to the location specified by the type parameter. It requires
the following payload: { "type": "tracker" }

The following is a list of supported navigation locations:

-   tracker toastrPop - asks Clockify to show custom toast messages on
    the UI. It requires the following payload: { "type": "info" \|
    "warning" \| "success" \| "error", "message": "your message" }

Toast messages will be shown on the bottom-right section of the screen.
The color of the background depends on the message type.

The following screenshot displays how an error toast would look like in
the UI:

Toast Message Example

Javascript Example

The following code example asks Clockify to display an error toast
message like in the screenshot above.

window.top?.postMessage(JSON.stringify({ action: "toastrPop", payload: {
type: "error", message: "Your add-on toast message" } }), "\*");

Previous Page Environment and Regions Next Page Development Checklist

ON THIS PAGE Event subscription Event dispatch © 2025 CAKE.com Inc. All
Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Development Checklist# The following is a checklist that will
help you go through the most important aspects that need to be accounted
for when developing an add-on.

Checklist Description Configurations Ensure add-on is properly
configured for the production environment Loading time Ensure your UI
components are loaded without unnecessary delays Token verification
Ensure add-on tokens are valid before accepting them Token types Ensure
that the correct token is used for each use case Installation token
Ensure the installation token is handled properly Webhooks verification
Ensure webhook signatures are valid Lifecycle verification Ensure
lifecycle signatures are valid Testing on multiple environments Ensure
that your add-on is tested on multiple workspaces and with multiple user
accounts Handling lifecycle events Ensure lifecycle events are handled
properly, and the information is persisted and deleted as needed
Handling webhooks Ensure webhook events are handled properly and respond
in a timely manner User preferences and UX Ensure the UX of the add-on
matches with the user's preferences on Clockify Environment#
Configurations# You must ensure that the release version of your add-on
is configured properly for the production environment. Things that need
to be checked include but are not limited to:

the manifest data the public base URL in case you have multiple
environments for you add-on (ex: development and production) external
configurations like database connections and API keys for external
services registered UI components Clockify endpoint URLs In most cases
an add-on will have multiple environments and generate its manifest
dynamically, with the base URL property being specified as an external
configuration property.

Clockify endpoint URLs are always dependent on the environment in which
the add-on is installed. You can learn more about Clockify environments
on the environments and regions page.

You must ensure your production add-on does not contain hardcoded values
like URLs, IDs or tokens which can break its functionality and make it
vulnerable to security attacks.

Loading time# To provide a seamless integration for your users, you must
ensure that the UI components for your add-on are loaded in a timely
manner. This includes but is not limited to:

only serving assets that are required for the add-on to function
compressing and serving media assets in sensible sizes serving minified
versions of static assets (JS and CSS) Security# Token Verification# You
should verify the signatures of every token that the add-on receives
before accepting it as valid. All tokens that are signed by Clockify
will be JWT tokens signed with RSA256. You can read more about the
verification on the Authentication & Authorization section.

The following is a checklist of items that should be checked when
verifying a token:

token signature is verified token algorithm is RS256 iss claim is
'clockify' type claim is 'addon' sub claim matches with the addon
manifest key token is not expired Additionally, a token might be valid,
but it might not be signed on behalf of the expected workspace. For
instance, a token that is signed on behalf of an add-on on workspace A
might be used to gain access to resources belonging to workspace B.

You should use the following claims to verify that a request matches the
specific workspace for which it was intended:

addonId - the ID of the installation on the workspace workspaceId - the
ID of the workspace Token types# You should ensure the correct token
type is used depending on the needs. Generally you should aim to use the
token with the least access, which is the user token, wherever possible.

User tokens can be used both on the backend and frontend parts of an
add-on. Installation tokens should never be used on the frontend.

Installation token# As described on the Authentication & Authorization
section, the installation token is a type of token that has full access
over a workspace and does not expire.

Due to the sensitive nature of its scope and the fact that it does not
expire, care should be taken not to leak the installation token to an
end user, to the UI of the add-on or to a third party.

The installation token should never be logged. If present in request
headers, you should ensure that it's properly redacted.

Webhooks verification# You should verify the signatures of every webhook
event you receive before accepting it as valid. You can read more on how
to verify the signature of a webhook on the webhooks section. It is also
recommended that you validate information from the payload against the
claims of the provided token.

For example, you should ensure the workspace ID referenced in the
payload of a time off request matches with the workspace ID that is part
of the token claims.

Lifecycle verification# Similar to webhooks, lifecycle signatures must
also be verified before being accepted as valid. You can read more about
the verification on the Authentication & Authorization section.

Add-on behavior# Testing on multiple environments# You must ensure your
add-on works as expected on different environments and workspaces. The
CAKE.com Developer Portal provides access to pre-populated testing
environments where you can test how your add-on behaves. We recommend
that you thoroughly test your add-on's behavior on multiple workspaces
(by creating multiple developer accounts) and with multiple users and
access levels (each testing environment allows you to log in on behalf
of various pre-existing users).

You must ensure that your add-on does not leak data to unauthorised
workspaces or users.

Lifecycle# install - it is recommended that you persist the installation
payload as the installed event will only be dispatched once per
installation uninstall - it is recommended that your add-on takes care
to properly handle uninstall events, including: removing database
entries, cancelling scheduled jobs, invalidating tokens etc Webhooks#
All webhook events contain a signature under the clockify-signature
header. This signature must match with the corresponding add-on
installation. Additionally, the clockify-webhook-event-type header must
also match with the expected webhook event.

Timeout

You should aim to respond to webhook events in a timely manner. If you
must execute a long-running operation that is triggered by a webhook
event, you should schedule it to be executed asynchronously and avoid
blocking the request.

Add-on UX# Add-ons should support the configurations and the preferences
set by the user on Clockify.

These include but are not limited to:

theme (light/dark) language date and time formats timezone and location
currency settings Some of the above can be retrieved from the token
claims present in the user token.

Claims present in the user token are:

language theme Information about the other properties can be retrieved
by making an authenticated API request to the Clockify API.

Previous Page Window Events Next Page Publishing and Guidelines

ON THIS PAGE Environment Configurations Loading time Security Token
Verification Token types Installation token Webhooks verification
Lifecycle verification Add-on behavior Testing on multiple environments
Lifecycle Webhooks Add-on UX © 2025 CAKE.com Inc. All Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Publishing# Guidelines & best practices# What's the criteria
add-ons need to meet in order to be published?# We want to make sure
that every individual using any CAKE.com Marketplace add-on gets a
product that is useful, has high performance and uses their data
responsibly. That's why we've compiled a set of instructions and
guidelines that can help you build the add-on that meets these
standards. Prior to building, please, take a couple of minutes and read
through these to make sure that the add-on you want to build complies
with the requirements listed below.

What are general guidelines for add-on content?# Become familiar with
the system.

Clockify is a complex system consisting of many components with all
sorts of dependencies between them: Timesheet, Timetracker, Calendar,
Report, Project, etc. They all work together and complement each other
perfectly. That's why, it's important you're familiar with all the
entities and patterns that Clockify comprises, so that you can use them
in a way that Clockify's customers are familiar with.

To better understand elements used to build Clockify, check out
Clockify's Glossary of terms.

Strive to enhance customer experience.

If you're already our customer, you can start building an app that will
benefit you and your team, solve your problems and later share it with
everyone else. If you've never used our product, we recommend you create
a free account. Try to build something new and unique.

Be clear about the purpose.

Sharing data with a third party can be scary for some users. Therefore,
you should be clear on how what you built adds value to their customer
experience and state clearly what their data will (and won't) be used
for.

Don't mislead your users.

Make sure you clearly stated add-on's purpose and necessary details when
creating your first version. You should also provide clear descriptions,
screenshots and videos in order to describe your add-on accurately.

Explore our branding guidelines.

Since an add-on you're building will be part of an existing product, we
suggest you take a look at our product branding guidelines and check if
the assets you built follow these guidelines before you submit an add-on
for a review.

Make an easy setup.

More people are likely to use an add-on that's easy to set up. This is
one of the crucial characteristics that attracts new users. For
instance, if using your product requires an account, it should be
created by getting the credentials from Clockify. If you need to ask
customers to sign up manually, you should pre-fill some user
information, e.g. user email.

Inform user of a third-party account.

If, in order to use add-on, user needs to create a separate third-party
account (i.e. log in to another service), this must be stated clearly to
the user in the description and the overview of the add-on.

Check grammar and spelling.

Add-on should not contain any typos, or grammatical errors. Therefore,
check grammar and spelling prior to submitting it for a review.

Is there any type of content that is prohibited?# To ensure that
CAKE.com Marketplace is a safe platform, we've created standards that
define and prohibit content that is harmful, inappropriate or offensive
to others.

Hate speech.

Add-ons that promote violence, or incite hatred against individuals, or
a group of individuals based on race, ethnicity, religion, age,
disability, nationality, sexual orientation, gender, gender identity, or
any other characteristic associated with discrimination will not be
published.

Violence.

We don't allow add-ons that facilitate or promote violence or
endangerment of an individual's safety.

Bullying and harassment.

Add-ons that contain or encourage threats, harassment and bullying will
not be published.

Harmful and malicious products.

Add-ons that promote or support any type of harmful and malicious
products won't be published.

Final note# Keep in mind that assets that contain any type of prohibited
content will not be published. Also, any prohibited, or harmful content
that is added after reviewing and publishing, and reported by users,
will be removed from the marketplace immediately. Please note that these
guidelines should be used in addition to our Terms of Use, Privacy
Policy, Security Requirements and Listing Agreement. Be sure to read all
of these documents. Also, keep in mind that these guidelines evolve
along with our business. Feel free to get in touch if you have any
feedback, or suggestions.

Create add-on# How to create add-on?# You'll have complete independence
and autonomy over building your add-on. This means that you'll build it
locally, but before submitting your code for a review, you should test
it in the testing environment provided to you. Also, check out our REST
API documentation and Development Toolkit section to get a better
insight on how to build and maintain add-ons. Add-on then needs to be
approved by our developers in the reviewing process and then it can be
published. To make sure the whole process goes smoothly and your request
is approved shortly, we will provide you with the necessary requirements
and standards all CAKE.com add-ons should meet in order to be published.

When creating an add-on you're actually creating the first version of
your add-on. All subsequent iterations will be defined as versions in
the Marketplace (e.g. v 1.0.1).

To create add-on:

Log in or sign up for your developer account Go to Add-ons page Click
CREATE ADD-ON Now, complete a two-step process and enter the required
data.

Basic information Create Add-on

Add-on icon: Upload image Add-on name: Choose a descriptive name that
users can easily understand Add-on manifest: Enter URL Product: Refers
to the application for which you are creating add-on At the moment, you
can only choose Clockify. You'll be able to create add-ons for Pumble
and Plaky soon.

Visibility: Public -- Add-on is visible to everyone and allows users
within the CAKE.com community to share it. At the moment, add-ons can
only be public. Private add-ons are coming soon.

Pricing model: Define pricing model for your add-on. Free Paid Click
NEXT to proceed to the second step of the form.

You can always save the form as draft and edit it later.

Listing Information you add in the listing will be available in the
Marketplace. Use it to help drive users to your add-on.

Listing Add-on

Choose category: Define a niche or business sector you'd like to improve
by building this add-on. In the Marketplace, published add-on will be
placed in the category you selected here. Short description: Give a
short overview of the purpose of your add-on and solution it provides
Long description: Explain what your add-on is providing in greater
detail and the value it brings to users Privacy policy: Define privacy
policy and clearly disclose which information you're collecting in this
manner Terms of use (optional): Along with privacy policy, you can also
add terms of use users need to agree to, in order to proceed with
installation Please keep in mind that, if there are some contradictory
statements in terms of use you provided here, to those written by our
legal team, CAKE.com's legal policy should be followed.

Website (optional): Add a link to the website that contains more
information about add-on. Creating a website that promotes your add-on
can help you attract more users. Add-on gallery (optional): Upload a set
of images, or screenshots that reflect the purpose and promote your
add-on in the best way. You can upload up to 5 images. Demo video
(optional): Add demo to spread the word, boost traffic, or use it as an
instructional video. Video needs to be publicly accessible on YouTube.
We encourage you to provide some additional instructions, including
information that would be relevant to the person doing an add-on review
like some additional notes or any other kind of specific guidelines.

Finally, by checking the Marketplace Listing Terms box you agree that
you'll meet and oblige the listing terms stated there as long as your
add-on is part of the CAKE.com Marketplace.

Information provided in this step will be published and available along
with the add-on on the Marketplace website.

Information provided in the two-step forms above, must be written in
English.

After you've completed the two steps, click the SUBMIT FOR REVIEW button
and your add-on will be submitted to the CAKE.com Marketplace team for a
review.

Once you've submitted your add-on for a review, our team will start the
reviewing process soon as possible.

How does reviewing work?# In order to successfully submit your add-on
version for a review, all the fields need to be filled out correctly and
there should be no pending requests.

Once you click the SUBMIT FOR REVIEW button, a new add-on version
request will be created. Request will get a pending status and will be
sent to our team for a review. You will get an email that confirms you
have successfully submitted a request for new version.

Reviewing process will take approximately two weeks.

When it's done, you will receive an email informing you that your
request is approved, or denied. The request will also be appropriately
marked as approved or denied in your profile.

If denied, we'll send you the feedback notes from our team that could
help you understand why the request was denied. You'll also be able to
see the request, edit it based on the feedback you received and submit
it for a review again.

If approved, you can continue and publish the version directly from the
email you received, by clicking PUBLISH. You'll receive another
confirmation email that will inform you that your add-on is published
and available in the marketplace. Click View \[addonname\] to see it in
the marketplace.

Create version & publish# How to create new version?# Every created
add-on must have a version. Versioning add-ons helps you create and
improve add-on's functionality easily and gradually without causing
inconvenience to your users.

In order to create a new version, at least one version of an add-on
needs to be published. Learn more about how to create add-on's first
version on Create Add-on section above.

First version will be automatically created as version 1.0.0. For each
new request, you need to manually add the version number. Version number
should reflect complexity of the functionalities with major version
being marked as e.g. 1.1.0 and minor version as e.g. 1.0.1. Also, keep
in mind that version numbers should be marked in a logical progression,
e.g. 2.0.0 version cannot be followed by version 1.5.0.

Creating new version is a three-step process similar to the one used to
create an add-on. However, when creating new version, add-on name and
icon in the Basic information page are generated from the published
version, but can be edited. Steps two and three are the same.

Which status can version request have?# Based on its lifecycle stage,
status of add-on's version request can be:

Draft Version request is in draft if you haven't completed all the steps
when creating an add-on, but you saved information you entered by
clicking the Save as draft button.

Pending When you submit the version for a review, its status is updated
to Pending. After review, the version can be approved or declined with
status changing accordingly.

There can be only one pending request per add-on.

Declined After your version is reviewed, it can be approved or declined.
If your version is declined, along with that information, you will also
get an explanation on why. You can edit the declined version based on
this feedback and submit it for a review again.

Approved An approved version is approved by our team. It goes further
into the publishing cycle and is ready to be published.

Published This is the final step in the publishing cycle. Once the
version is approved, it can be published and when published it becomes
available in the marketplace for everyone to use. After you published
one add-on version, you can proceed by creating a new one.

Once your add-on (first, or any subsequent version) has been published
on the CAKE.com Marketplace, you'll receive a confirmation email saying
that your add-on version has been successfully published.

Add-on can have only one published version.

If an add-on is already in the marketplace, once its new version request
is published, the existing published version immediately becomes
outdated.

Add-on Statuses

Outdated If a newer version is published, the existing one becomes
automatically deprecated and outdated.

Unlisted This status is added to the published version that has been
removed from the marketplace and can't be used or installed by new
users. However, those who use the unlisted version can still see and use
it.

Delete add-on# You can delete an entire add-on with all of its versions
regardless of their status, or just an add-on's version request. You can
do both of these actions from the Add-ons page, in your profile.
However, deleting an entire add-on requires sending a request to our
support team.

How to delete add-on?# If you'd like to delete your add-on, you can do
that from the Add-ons page.

To delete an add-on:

Click Delete add-on in the bottom right corner Delete add-on modal
appears Click SEND REQUEST Add-on Delete

After you've sent the request to delete an add-on, our support team will
contact you shortly, and ask for confirmation. Soon after your
confirmation, the process of deletion will start. Add-on users will
receive an email that informs them of deletion and you will receive an
email that notifies you that the process has begun.

Once the process has started, add-on is unlisted, removed from the
marketplace and unavailable for new users. The existing users will be
able to use the add-on until it is deleted. To ensure that they are
informed, after the deletion process is set in motion, information of
the deletion and add-on's retirement date will be prominently displayed
at the top of the page throughout the application.

The process of deletion lasts 31 days. An add-on will be deleted at the
end of the last day at midnight. This process cannot be canceled or
reversed.

After it is deleted:

Add-on will be deleted from the marketplace Your profile and all users
will lose access to it Subscription will be canceled for paid add-ons
How to delete add-on version request?# If you don't want to continue
with the process of creating a new add-on version, you can delete it in
the request preview table, in the Add-ons page.

When deleting an add-on version, depending on the status of the version,
different actions are available.

If version is in:

Draft If this is the only version request for this add-on, by deleting
it, you will delete an entire add-on and if not, you will delete only
draft version of this add-on.

Pending If this is the only version request for this add-on, by deleting
it, you will delete an entire add-on and if not, you will delete only
draft version of this add-on.

Approved If this is the only version request for this add-on, by
deleting it, you will delete an entire add-on and if not, you will
delete only draft version of this add-on.

Declined If this is the only version request for this add-on, by
deleting it, you will delete an entire add-on and if not, you will
delete only draft version of this add-on.

Outdated If this is the only version request for this add-on, by
deleting it, you will delete an entire add-on and if not, you will
delete only draft version of this add-on.

You cannot delete the Unlisted and Published add-on versions since that
action would affect potential users of those add-ons.

Data privacy guidelines# As you'll have access to customer information
which includes personal data, it's important how you store and handle
the data. Personal data is sensitive and in many cases regulated by
applicable laws. Listed below are best practices you should follow, in
addition to the Terms of Use, when developing the add-on on the CAKE.com
marketplace.

Clearly explain your data privacy practices# When submitting your add-on
for review you are required to provide a Privacy policy which should
explain to the users how you plan on using their data. The privacy
policy should clearly explain to the user what data the add-on will
collect, how that data will be used, who will have access to the data
and explain the user's choice.

Minimize the data you collect# Collect data only where you need it. Do
not collect the data because you think it may be useful later. Where
personal data is involved, consider de-identifying it. Also consider
deleting user data when they request it or when they uninstall your
add-on. Have in mind that you don't need to store data indefinitely, put
some data retention schemes in place.

Get consent for certain data use# When submitting an add-on for review
you'll have to check which data you collect in the permissions tab. You
should only collect the data you checked in that tab. In this way users
consent to the usage of their data when installing an add-on, but only
for the purpose of an add-on. Using data for marketing, sharing data
with third-parties and other data use cases not strictly required to
support the operation of your add-on may require a separate consent from
the user before collecting or using the data. As a general rule of
thumb, you should always get consent if the user would not expect their
data to be used or shared in a particular way given the purpose of your
add-on.

Consent may not be embedded in a privacy policy. Instead, it must be
collected from the user directly. You are responsible for collecting and
maintaining all such consents, either through the add-on itself or
through direct communication with the add-on user.

Note, regardless of whether you obtain consent, some data use cases may
be prohibited by the Terms of Use. You are responsible for reviewing and
complying with those terms.

Provide access, modification and erasure of personal data# Applicable
laws and data management best practices require that you make it easy
for users to get a copy of, correct and delete their personal data. This
means, if you are storing personal data, you need to know where that
personal data is at all times and be able to update it or remove it upon
request.

Offer additional data processing terms# If you are accessing, storing or
otherwise processing personal data of EEA residents, users may request
that you sign and comply with additional data protection terms,
consistent with Article 28 of the General Data Protection Regulation
("GDPR"). You are responsible for understanding and complying with the
terms required under Article 28 of the GDPR as it relates to the user
data you access, store or otherwise process in connection with the
user's consent to install and share data with your add-on.

Invest in data security# You must take reasonable steps to protect user
data shared with you and collected by your add-on, including user device
information. We recommend you to follow our Security guidelines for a
more comprehensive list for securing your add-on.

In the event your add-on or suppliers experience a data security breach,
you are responsible for communicating with users and regulators, as
required by applicable law. It's also important to let us know of the
incident by emailing to support@cake.com.

Security requirements# Authentication & authorization# An add-on must
authenticate and authorize every request on all endpoints exposed.
Anonymous access to application endpoints and resources can be allowed
in scenarios where it is needed.

Data protection# Any CAKE.com End User Data stored by an application
outside of the CAKE.com product or users' browser must ensure full disk
encryption at-rest. If accessed by an application or a service, it
should be authenticated and authorized appropriately. An application
must use TLS version 1.2 (or higher) to encrypt all of its traffic, and
enable HSTS with a minimum age of one year. An application must follow
the "Principle of Least Privilege", when requesting app scopes. This
means that an application should only request scopes required to perform
its intended functionality, and nothing more. An application must
securely store and manage secrets, which include OAuth tokens,
sharedSecret, API keys, and encryption keys. They cannot be stored in
places that are easily accessible. Examples of places include: Source
code and code repository tools, such as Bitbucket and Github URL strings
Referer headers Application logs Application security# An application
must maintain and securely configure domains where the application is
hosted. When applicable, an application must enable security headers and
cookie security attributes. An application must validate and sanitize
all untrusted data and treat all user input as unsafe to mitigate
injection-related vulnerabilities. Untrusted data is any input that can
be manipulated to contain a web attack payload. An application must not
use versions of third-party libraries and dependencies with known
critical or high vulnerabilities. When vulnerabilities in these
libraries and dependencies are discovered, application developers must
remediate them as quickly as possible. Privacy# An application must not
collect or store credentials belonging to CAKE.com user accounts such as
user passwords or user API tokens. Vulnerability management# You must
notify CAKE.com of all security incidents via support@cake.com. Your
account email will be taken as a security contact where you'll be
notified about vulnerabilities in the app. Previous Page Development
Checklist Next Page Private addon deployment

ON THIS PAGE Guidelines & best practices What's the criteria add-ons
need to meet in order to be published? What are general guidelines for
add-on content? Is there any type of content that is prohibited? Final
note Create add-on How to create add-on? How does reviewing work? Create
version & publish How to create new version? Which status can version
request have? Delete add-on How to delete add-on? How to delete add-on
version request? Data privacy guidelines Clearly explain your data
privacy practices Minimize the data you collect Get consent for certain
data use Provide access, modification and erasure of personal data Offer
additional data processing terms Invest in data security Security
requirements Authentication & authorization Data protection Application
security Privacy Vulnerability management © 2025 CAKE.com Inc. All
Rights Reserved.

Terms Privacylogo

Enter text

⌘K Developer portal

More resources logo LEARN Introduction Quick Start BUILD Manifest
Lifecycle UI Components Webhooks Settings Developer Account
Authentication and Authorization Environment and Regions Window Events
PUBLISH Development Checklist Publishing and Guidelines Private addon
deployment Private Add-ons Documentation# Introduction to Private
Add-ons# Private add-ons allow developers to create solutions accessible
only to specific workspaces, ideal for internal testing and use. This
feature encourages users with custom integrations to transition to
managed add-ons, enhancing security.

Creating a Private Add-on# Steps to Create:# Set Visibility: Set
Visibility: During creation, select "Private" visibility. Whitelist
Workspaces: Define up to three workspaces by their IDs for access.
Manifest key: If you already have the add-on in production, the manifest
key needs to be different since the private add-on is technically a new
add-on. Note: You can find your workspace ID by going to the workspace
settings.

Managing a Private Add-on# Updating Versions# Maintain whitelists across
versions unless changes are needed. Upon removing a workspace from
whitelists, the add-on is automatically uninstalled. When a new
workspace is added, an email is sent notifying the user about the
add-on. Publishing a Private Add-on# Key Differences:# No Payment Setup:
Skip payment configuration. No Vendor Profile: Not required for private
distribution. No Review Process: Immediate publication post-submission.
Can delete a Private Add-on without waiting. Deleting a Private Add-on:#
Deletion is immediate. All installations are removed upon deletion.
Installing a Private Add-on# Admin Instructions:# Receive URL: Obtain
installation link via email notification. Install: Use provided URL to
install the add-on. Visibility: See "Private" status in Clockify,
distinguishing from public versions. Previous Page Publishing and
Guidelines

ON THIS PAGE Introduction to Private Add-ons Creating a Private Add-on
Steps to Create: Managing a Private Add-on Updating Versions Publishing
a Private Add-on Key Differences: Deleting a Private Add-on: Installing
a Private Add-on Admin Instructions: © 2025 CAKE.com Inc. All Rights
Reserved.

Terms Privacy
