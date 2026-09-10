const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function: onComplaintStatusChange
 * Triggers whenever a document in the "complaints" collection is updated in Cloud Firestore.
 * If status or worker notes change, retrieves citizen's fcmToken and sends a FCM Push Notification.
 */
exports.onComplaintStatusChange = functions.firestore
  .document("complaints/{complaintId}")
  .onUpdate(async (change, context) => {
    const newData = change.after.data();
    const oldData = change.before.data();

    // Check if status or notes changed
    if (
      newData.status === oldData.status &&
      newData.workerNotes === oldData.workerNotes
    ) {
      return null;
    }

    const citizenId = newData.citizenId;
    if (!citizenId) {
      console.log("No citizenId present on complaint document");
      return null;
    }

    // Retrieve citizen profile from Firestore
    const userDoc = await admin.firestore().collection("users").doc(citizenId).get();
    if (!userDoc.exists) {
      console.log(`User ${citizenId} not found in Firestore`);
      return null;
    }

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
      console.log(`No fcmToken registered for user ${citizenId}`);
      return null;
    }

    const payload = {
      token: fcmToken,
      notification: {
        title: `Report Update: ${newData.issueType || "Civic Complaint"}`,
        body: `Status updated to ${newData.status}.${newData.workerNotes ? " Notes: " + newData.workerNotes : ""}`
      },
      data: {
        complaintId: newData.id || context.params.complaintId,
        status: newData.status || "",
        click_action: "FLUTTER_NOTIFICATION_CLICK"
      }
    };

    try {
      const response = await admin.messaging().send(payload);
      console.log(`Successfully dispatched notification to citizen ${citizenId}:`, response);
      return response;
    } catch (error) {
      console.error(`Error sending push notification to citizen ${citizenId}:`, error);
      return null;
    }
  });

/**
 * Cloud Function: onWorkerTaskAssigned
 * Triggers when a worker task is created or updated in the "workerTasks" collection.
 * Sends an FCM Push Notification to the assigned municipal worker.
 */
exports.onWorkerTaskAssigned = functions.firestore
  .document("workerTasks/{taskId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;

    const task = change.after.data();
    const assignedWorkerId = task.assignedWorkerId;

    if (!assignedWorkerId) return null;

    const workerDoc = await admin.firestore().collection("users").doc(assignedWorkerId).get();
    if (!workerDoc.exists) return null;

    const fcmToken = workerDoc.data().fcmToken;
    if (!fcmToken) return null;

    const payload = {
      token: fcmToken,
      notification: {
        title: "New Municipal Task Assigned",
        body: `Task #${task.complaintId || context.params.taskId} (${task.issueType}) at ${task.address || "Location"}`
      },
      data: {
        taskId: task.id || context.params.taskId,
        priority: task.priority || "NORMAL"
      }
    };

    try {
      const response = await admin.messaging().send(payload);
      console.log(`Successfully notified worker ${assignedWorkerId}:`, response);
      return response;
    } catch (error) {
      console.error(`Error notifying worker ${assignedWorkerId}:`, error);
      return null;
    }
  });
