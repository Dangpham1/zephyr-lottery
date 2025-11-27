package com.example.zephyr_lottery.repositories;

import android.util.Log;

import com.example.zephyr_lottery.models.Participant;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class manages the statuses of participants and the accepting/declining of invitations.
 */
public class EventRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Update a participant’s status in Firestore. */
    public Task<Void> updateParticipantStatus(String eventId, String userId, String status) {
        DocumentReference participantRef = db.collection("events")
                .document(eventId)
                .collection("participants")
                .document(userId);

        Participant p = new Participant(userId, status, null, Timestamp.now());
        return participantRef.set(p, SetOptions.merge());
    }

    /** Accept an invitation (sets status to "accepted"). */
    public void acceptInvitation(String eventId, String userId,
                                 Runnable onSuccess, Consumer<Exception> onError) {
        updateParticipantStatus(eventId, userId, "accepted")
                .addOnSuccessListener(v -> {
                    Log.d("EventRepo", "Accepted invitation");
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Accept failed", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Decline an invitation (sets status to "declined" and invite next from waiting list). */
    public void declineInvitation(String eventId, String userId,
                                  Runnable onSuccess, Consumer<Exception> onError) {
        updateParticipantStatus(eventId, userId, "declined")
                .addOnSuccessListener(v -> {
                    Log.d("EventRepo", "Declined invitation");
                    inviteNextFromWaitingList(eventId, onSuccess, onError);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Decline failed", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Invite the next participant from the waiting list (oldest joinedAt first). */
    public void inviteNextFromWaitingList(String eventId,
                                          Runnable onSuccess,
                                          Consumer<Exception> onError) {
        CollectionReference waitingRef = db.collection("events")
                .document(eventId)
                .collection("waitingList");

        waitingRef.orderBy("joinedAt", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    List<DocumentSnapshot> docs = query.getDocuments();
                    if (docs.isEmpty()) {
                        Log.d("EventRepo", "No waiting list entrants");
                        if (onSuccess != null) onSuccess.run();
                        return;
                    }
                    DocumentSnapshot next = docs.get(0);
                    String nextUserId = next.getId();

                    WriteBatch batch = db.batch();
                    DocumentReference participantRef = db.collection("events")
                            .document(eventId)
                            .collection("participants")
                            .document(nextUserId);

                    Participant invited = new Participant(
                            nextUserId, "SELECTED", Timestamp.now(), Timestamp.now()
                    );

                    batch.set(participantRef, invited, SetOptions.merge());
                    batch.delete(next.getReference()); // remove from waiting list

                    batch.commit()
                            .addOnSuccessListener(bv -> {
                                Log.d("EventRepo", "Invited next entrant: " + nextUserId);
                                if (onSuccess != null) onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EventRepo", "Failed inviting next entrant", e);
                                if (onError != null) onError.accept(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Read waiting list failed", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Listen to a participant’s status so the UI can react in real time. */
    public ListenerRegistration listenToParticipantStatus(String eventId, String userId,
                                                          Consumer<String> onStatus) {
        DocumentReference ref = db.collection("events")
                .document(eventId)
                .collection("participants")
                .document(userId);

        return ref.addSnapshotListener((snap, error) -> {
            if (error != null) {
                Log.e("EventRepo", "Listen error", error);
                return;
            }
            String status = (snap != null && snap.exists()) ? snap.getString("status") : null;
            if (onStatus != null) onStatus.accept(status);
        });
    }

    /** Notify all entrants whose status is "SELECTED". */
    public void notifyAllSelectedEntrants(String eventId,
                                          Runnable onSuccess,
                                          Consumer<Exception> onError) {
        CollectionReference participantsRef = db.collection("events")
                .document(eventId)
                .collection("participants");

        participantsRef.whereEqualTo("status", "SELECTED")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.d("EventRepo", "No selected entrants to notify for event " + eventId);
                        if (onSuccess != null) onSuccess.run();
                        return;
                    }
                    for (DocumentSnapshot doc : query) {
                        String userId = doc.getId();
                        sendNotificationToUser(
                                userId,
                                "Congratulations! You have been selected for event " + eventId
                        );
                        logNotificationSent(eventId, userId, "SELECTED", null, null);
                    }
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Failed to fetch selected entrants", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Notify all entrants on the waiting list (status = PENDING). */
    public void notifyAllWaitingListEntrants(String eventId,
                                             Runnable onSuccess,
                                             Consumer<Exception> onError) {
        CollectionReference participantsRef = db.collection("events")
                .document(eventId)
                .collection("participants");

        participantsRef.whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.d("EventRepo", "No waiting list entrants to notify for event " + eventId);
                        if (onSuccess != null) onSuccess.run();
                        return;
                    }
                    for (DocumentSnapshot doc : query) {
                        String userId = doc.getId();
                        sendNotificationToUser(
                                userId,
                                "You are on the waiting list for event " + eventId
                        );
                        logNotificationSent(eventId, userId, "PENDING", null, null);
                    }
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Failed to fetch waiting list entrants", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Notify all entrants whose status is "CANCELLED". */
    public void notifyAllCancelledEntrants(String eventId,
                                           Runnable onSuccess,
                                           Consumer<Exception> onError) {
        CollectionReference participantsRef = db.collection("events")
                .document(eventId)
                .collection("participants");

        participantsRef.whereEqualTo("status", "CANCELLED")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.d("EventRepo", "No cancelled entrants to notify for event " + eventId);
                        if (onSuccess != null) onSuccess.run();
                        return;
                    }
                    for (DocumentSnapshot doc : query) {
                        String userId = doc.getId();
                        sendNotificationToUser(
                                userId,
                                "Your participation in event " + eventId + " has been cancelled"
                        );
                        logNotificationSent(eventId, userId, "CANCELLED", null, null);
                    }
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Failed to fetch cancelled entrants", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Record a notification in an audit trail (US03.08.01). */
    public void logNotificationSent(String eventId, String userId, String notificationType,
                                    Runnable onSuccess, Consumer<Exception> onError) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("eventId", eventId);
        logEntry.put("userId", userId);
        logEntry.put("notificationType", notificationType);
        logEntry.put("sentAt", Timestamp.now());

        db.collection("notificationLogs")
                .add(logEntry)
                .addOnSuccessListener(docRef -> {
                    Log.d("EventRepo", "Logged notification: " + docRef.getId());
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepo", "Failed to log notification", e);
                    if (onError != null) onError.accept(e);
                });
    }

    /** Placeholder for actual Firebase Cloud Messaging integration. */
    private void sendNotificationToUser(String userId, String message) {
        Log.d("EventRepo", "Sending notification to " + userId + ": " + message);
        // In production, replace this log with actual FCM push logic.
    }
}
