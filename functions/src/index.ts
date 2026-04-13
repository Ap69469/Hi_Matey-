import * as admin from "firebase-admin";
import {setGlobalOptions} from "firebase-functions/v2";
import {onDocumentCreated} from "firebase-functions/v2/firestore";

admin.initializeApp();
setGlobalOptions({maxInstances: 10});

export const notifyPartnerOnNewTask = onDocumentCreated(
  "sharedTasks/{taskId}",
  async (event) => {
    const task = event.data?.data();
    if (!task) return;

    const assignedToUid = task.assignedToUid as string;
    const createdBy = task.createdBy as string;
    const title = task.title as string;

    if (!assignedToUid) {
      console.log("No assignedToUid, skipping notification");
      return;
    }

    // Get the assigned user's FCM token
    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(assignedToUid)
      .get();

    const fcmToken = userDoc.data()?.fcmToken as string;
    if (!fcmToken) {
      console.log("No FCM token for user:", assignedToUid);
      return;
    }

    // Send the notification
    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: "New task from your partner! 🏴‍☠️",
        body: `${createdBy} assigned you: "${title}"`,
      },
      android: {
        priority: "high",
      },
    });

    console.log("Notification sent to:", assignedToUid);
  }
);
