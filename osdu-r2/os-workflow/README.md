# OSDU Workflow Service

## Table of contents

* [Introduction](#introduction)
* [System interactions](#system-interactions)
* [API](#api)
* [GCP implementation](#gcp-implementation)

## Introduction

The OpenDES (OSDU) Workflow service starts any business process in the system, such as ingestion of
OSDU data. The Workflow service provides a wrapper functionality around the Apache Airflow functions
and is designed to carry out a preliminary work with files before running the Airflow Directed
Acyclic Graphs (DAGs).

In OSDU R2, depending on the types of data, workflow, and user, the Workflow service starts the
necessary workflow such as well log LAS files ingestion or opaque ingestion.

## System interactions

The Workflow service in the OSDU R2 Prototype defines two workflows:

* Starting ingestion of new files
* Delivering the current status of an ingestion job

### Ingestion

The ingestion workflow starts by a call to the `/startWorkflow` API. The following diagram shows the
workflow.

![OSDU Workflow Service startWorkflow API](https://gitlab.osdu-gcp.dev/OSDU/os-workflow/uploads/d2122ae7e53a234d92b87552e5d6b5b1/OSDU_R2_Workflow_Service_startWorkflow_API.png)

Upon the `/startWorkflow` request:

1. Validate the incoming request.
    * Check that the workflow type corresponds to the allowed values &mdash; "ingest" or "osdu".
    * Check that the data type corresponds to the allowed values &mdash; "well_log" or "opaque".
2. Query the database to obtain a DAG suitable for the current request. The Workflow service
decides which DAG to run by the following three parameters:
    * `WorkflowType`
    * `DataType`
    * `UserType`
3. Submit a new ingestion job to the Workflow Engine (Apache Airflow).
4. Store the workflow data in the database with the SUBMITTED workflow status.
5. Respond with the workflow ID to the Ingestion service.

### Get workflow status

1. Query the database with the workflow ID to obtain the workflow job status.
    * Respond with the `404 Not Found` status if the requested workflow ID isn't found.
2. Return the workflow job status to the user or application.

## API

### POST /startWorkflow

Starts a new workflow. This API isn't available for third-party applications.

The `/startWorkflow` endpoint is a wrapper around the Airflow invocation, and it can reconfigure the
default workflows. For each combination of user, data, and workflow types, the API identifies a
suitable DAG and then calls Airflow.

For the OSDU R2 implementation, the API doesn't reconfigure the workflows and only queries the
database to determine which DAG to run.

#### Request

The incoming request must contain the properties in the table below. All the given properties are
provided in the request body.

| Property     | Type     | Description                                                                |
| ------------ | -------- | -------------------------------------------------------------------------- |
| WorkflowType | `String` | Type of workflow job to run &mdash; "osdu" or "ingest"                     |
| DataType     | `String` | Type of data to be ingested &mdash; "well_log" or "opaque"                 |
| Context      | `List`   | Data required to run a particular DAG, provided as list of key-value pairs |

> The Context may include a file location, ACL and legal tags, and the Airflow run ID. The
> startWorkflow API passes the Context to Airflow without modifying it.

#### Response

| Property   | Type     | Description                   |
| ---------- | -------- | ----------------------------- |
| WorkflowID | `String` | Unique ID of the workflow job |

### POST /getStatus

Returns the current status of a workflow job.

#### Request

The incoming request must contain the following properties in the request body.

| Property   | Type     | Description                 |
| ---------- | -------- | --------------------------- |
| WorkflowID | `String` | Unique ID of a workflow job |

#### Response

If the workflow ID is found in the database, the following response is returned to the user.

| Property | Type     | Description                                                                        |
| -------- | -------- | ---------------------------------------------------------------------------------- |
| Status   | `String` | Current status of the workflow job &mdash; SUBMITTED, RUNNING, FINISHED, or FAILED |

If the workflow ID isn't found in the database, the `404 Not Found` response is returned.

## GCP implementation

The GCP Identity and Access Management service account for the Workflow service must have the
**Composer User** and **Cloud Datastore User** roles.

Obtaining user credentials for Application Default Credentials isn't suitable for the development
purposes because signing a blob is only available with the service account credentials. Remember to
set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable. Follow the [instructions on the
Google developer's portal][application-default-credentials].

## Firestore

Upon an ingestion request, the Workflow service needs to determine which DAG to run. To do that, the
service queries the database with the workflow type and data type.

The GCP-based implementation of the Workflow service uses Cloud Firestore with the following
`ingestion-strategy` and `workflow-status` collections.

### `ingestion-strategy`

The database needs to store the following information to help determine a DAG.

| Property     | Type     | Description                                                    |
| ------------ | -------- | -------------------------------------------------------------- |
| WorkflowType | `String` | One of two supported workflow types &mdash; "osdu" or "ingest" |
| DataType     | `String` | One of two supported data types &mdash; "well_log" or "opaque" |
| UserID       | `String` | A unique identifier of the user group or role                  |
| DAGName      | `String` | The name of the DAG                                            |

> **Note**: The current implementation of OSDU doesn't support the UserID property. When the
> security system is fully defined, the UserID field will store the ID of the user group or role.

### `workflow-status`

After a workflow starts, the Workflow service stores the following information in Firestore.

| Property     | Type     | Description                                                                      |
| ------------ | -------- | -------------------------------------------------------------------------------- |
| WorkflowID   | `String` | Unique workflow ID                                                               |
| AirflowRunID | `String` | Unique Airflow process ID generated by the Workflow service                      |
| Status       | `String` | Current status of a workflow job &mdash; SUBMITTED, RUNNING, FINISHED, or FAILED |
| SubmittedAt  | `String` | Timestamp when the workflow job was submitted to Workflow Engine                 |
| SubmittedBy  | `String` | ID of the user role or group. Not supported in OSDU R2                           |

[application-default-credentials]: https://developers.google.com/identity/protocols/application-default-credentials#calling