package com.example.zephyr_lottery.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zephyr_lottery.R;
import com.example.zephyr_lottery.repositories.EventRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class EventInvitationActivity extends AppCompatActivity {

    private EventRepository repo;
    private String eventId;
    private String userId;

    private Button btnAccept;
    private Button btnDecline;

    private ListenerRegistration statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_invitation);

        repo = new EventRepository();

        eventId = getIntent().getStringExtra("EVENT_ID");
        userId = getIntent().getStringExtra("USER_ID");

        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        btnAccept.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Accept invitation?")
                        .setMessage("Confirm you want to participate.")
                        .setPositiveButton("Accept", (d, w) ->
                                repo.acceptInvitation(eventId, userId,
                                        () -> Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show(),
                                        e -> Toast.makeText(this, "Failed to accept", Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        btnDecline.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Decline invitation?")
                        .setMessage("Confirm you want to decline. The next entrant may be invited.")
                        .setPositiveButton("Decline", (d, w) ->
                                repo.declineInvitation(eventId, userId,
                                        () -> Toast.makeText(this, "Declined", Toast.LENGTH_SHORT).show(),
                                        e -> Toast.makeText(this, "Failed to decline", Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        statusListener = repo.listenToParticipantStatus(eventId, userId, status -> {
            boolean invited = "SELECTED".equals(status);
            btnAccept.setEnabled(invited);
            btnDecline.setEnabled(invited);

            if ("accepted".equals(status) || "declined".equals(status) || status == null) {
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (statusListener != null) statusListener.remove();
    }
}