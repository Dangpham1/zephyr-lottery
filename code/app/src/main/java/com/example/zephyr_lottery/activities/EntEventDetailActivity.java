package com.example.zephyr_lottery.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.zephyr_lottery.R;
import com.example.zephyr_lottery.repositories.EventRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class EntEventDetailActivity extends AppCompatActivity {

    private Button back_event_details_button;
    private Button register_button;
    private Button leave_button;

    private TextView title;
    private TextView closingDate;
    private TextView startEnd;
    private TextView dayTime;
    private TextView location;
    private TextView price;
    private TextView description;
    private TextView entrantNumbers;
    private TextView lotteryWinners;
    private ImageView eventImageView;

    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private DocumentReference docRef;

    // New fields to handle invitation logic
    private EventRepository repo;
    private ListenerRegistration statusListener;
    private String currentEventId;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ent_eventdetail_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        title = findViewById(R.id.textView_ed_title);
        closingDate = findViewById(R.id.textView_closingdate);
        startEnd = findViewById(R.id.textView_startenddates);
        dayTime = findViewById(R.id.textView_day_time);
        location = findViewById(R.id.textView_location_string);
        price = findViewById(R.id.textView_price_string);
        description = findViewById(R.id.textView_description_string);
        entrantNumbers = findViewById(R.id.textView_currententrants);
        lotteryWinners = findViewById(R.id.textView_lotterywinners);
        eventImageView = findViewById(R.id.imageView_ent_eventImage);

        register_button = findViewById(R.id.button_register);
        leave_button = findViewById(R.id.button_leave_waitlist);
        back_event_details_button = findViewById(R.id.button_event_details_back);

        // Determine current user email (from intent or FirebaseAuth)
        String user_email = getIntent().getStringExtra("USER_EMAIL");
        if (user_email == null || user_email.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                user_email = currentUser.getEmail();
            }
        }
        final String currentUserEmail = user_email;

        // Save user ID and event ID as fields for use in onStart
        this.currentUserId = currentUserEmail;

        // Determine event ID (document id)
        String eventHash = getIntent().getStringExtra("EVENT");
        this.currentEventId = eventHash;

        // Set up Firestore references
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        docRef = eventsRef.document(eventHash);

        // Initialize the repository
        repo = new EventRepository();

        // Load event details onto the screen
        loadEventDetails();

        // Set up button listeners
        register_button.setOnClickListener(view -> {
            // join waiting list logic (unchanged)
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                Toast.makeText(
                        EntEventDetailActivity.this,
                        "Unable to determine your account. Please sign in again.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            docRef.get().addOnSuccessListener(currentEvent -> {
                if (!currentEvent.exists()) {
                    Toast.makeText(
                            EntEventDetailActivity.this,
                            "Event not found.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                List<String> entrantsList = (List<String>) currentEvent.get("entrants");
                if (entrantsList == null) {
                    entrantsList = new ArrayList<>();
                } else {
                    entrantsList = new ArrayList<>(entrantsList);
                    entrantsList.remove(null);
                }

                if (entrantsList.contains(currentUserEmail)) {
                    Toast.makeText(
                            EntEventDetailActivity.this,
                            "You are already on the waiting list.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Long limitLong = currentEvent.getLong("limit");
                boolean hasLimit = limitLong != null;
                int limitValue = hasLimit ? limitLong.intValue() : Integer.MAX_VALUE;
                if (hasLimit && entrantsList.size() >= limitValue) {
                    Toast.makeText(
                            EntEventDetailActivity.this,
                            "This event is full.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                int currentSize = entrantsList.size();
                String limitDisplay = hasLimit ? String.valueOf(limitValue) : "?";

                docRef.update("entrants", FieldValue.arrayUnion(currentUserEmail))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(
                                    EntEventDetailActivity.this,
                                    "Joined waiting list.",
                                    Toast.LENGTH_LONG
                            ).show();
                            int newCount = currentSize + 1;
                            entrantNumbers.setText(
                                    "Current Entrants: " + newCount + "/" + limitDisplay + " slots"
                            );
                        })
                        .addOnFailureListener(e -> {
                            Log.e("EntEventDetail", "Error adding entrant", e);
                            Toast.makeText(
                                    EntEventDetailActivity.this,
                                    "Failed to join. Please try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            });
        });

        leave_button.setOnClickListener(view -> {
            // leave waiting list logic (unchanged)
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                Toast.makeText(
                        EntEventDetailActivity.this,
                        "Unable to determine your account. Please sign in again.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            docRef.get().addOnSuccessListener(currentEvent -> {
                if (!currentEvent.exists()) {
                    Toast.makeText(
                            EntEventDetailActivity.this,
                            "Event not found.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                List<String> entrantsList = (List<String>) currentEvent.get("entrants");
                if (entrantsList == null) {
                    entrantsList = new ArrayList<>();
                } else {
                    entrantsList = new ArrayList<>(entrantsList);
                    entrantsList.remove(null);
                }

                if (!entrantsList.contains(currentUserEmail)) {
                    Toast.makeText(
                            EntEventDetailActivity.this,
                            "You are not on the waiting list.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Long limitLong = currentEvent.getLong("limit");
                String limitDisplay = limitLong != null ? String.valueOf(limitLong) : "?";
                int currentSize = entrantsList.size();

                docRef.update("entrants", FieldValue.arrayRemove(currentUserEmail))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(
                                    EntEventDetailActivity.this,
                                    "You have left the waiting list.",
                                    Toast.LENGTH_LONG
                            ).show();
                            int newCount = Math.max(0, currentSize - 1);
                            entrantNumbers.setText(
                                    "Current Entrants: " + newCount + "/" + limitDisplay + " slots"
                            );
                        })
                        .addOnFailureListener(e -> {
                            Log.e("EntEventDetail", "Error removing entrant", e);
                            Toast.makeText(
                                    EntEventDetailActivity.this,
                                    "Failed to leave. Please try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            });
        });

        // Back button logic (unchanged)
        String finalUserEmailForBack = currentUserEmail;
        back_event_details_button.setOnClickListener(view -> {
            String fromActivity = getIntent().getStringExtra("FROM_ACTIVITY");
            Intent intent;

            if ("MY_EVENTS".equals(fromActivity)) {
                intent = new Intent(EntEventDetailActivity.this, EntEventHistoryActivity.class);
            } else if ("HOME_ENT".equals(fromActivity)) {
                //return to entrant home (path taken if qr code was scanned)
                intent = new Intent(EntEventDetailActivity.this, HomeEntActivity.class);
            } else {
                intent = new Intent(EntEventDetailActivity.this, EntEventsActivity.class);
            }

            intent.putExtra("USER_EMAIL", finalUserEmailForBack);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Listen for changes to the participant's status
        if (currentEventId != null && currentUserId != null && !currentUserId.isEmpty()) {
            statusListener = repo.listenToParticipantStatus(currentEventId, currentUserId, status -> {
                if ("SELECTED".equals(status)) {
                    // Launch the invitation screen when selected
                    Intent intent = new Intent(EntEventDetailActivity.this, EventInvitationActivity.class);
                    intent.putExtra("EVENT_ID", currentEventId);
                    intent.putExtra("USER_ID", currentUserId);
                    startActivity(intent);
                } else if ("accepted".equals(status)) {
                    // Already accepted – disable waiting list buttons
                    register_button.setEnabled(false);
                    leave_button.setEnabled(false);
                    Toast.makeText(this, "You have accepted the invitation.", Toast.LENGTH_SHORT).show();
                } else if ("declined".equals(status)) {
                    // Already declined – disable waiting list buttons
                    register_button.setEnabled(false);
                    leave_button.setEnabled(false);
                    Toast.makeText(this, "You have declined the invitation.", Toast.LENGTH_SHORT).show();
                }
                // If null or "PENDING", do nothing – user is still on the waiting list.
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listener to prevent memory leaks
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
    }

    /** Loads the event details from Firestore and updates the UI. */
    private void loadEventDetails() {
        docRef.get().addOnSuccessListener(currentEvent -> {
            if (!currentEvent.exists()) {
                Log.e("Firestore", "Event not found.");
                return;
            }

            title.setText(currentEvent.getString("name"));
            closingDate.setText("");
            startEnd.setText("");

            String weekdayString = currentEvent.getString("weekdayString");
            String timeString = currentEvent.getString("time");
            StringBuilder dayTimeText = new StringBuilder();
            if (weekdayString != null) {
                dayTimeText.append(weekdayString).append(" ");
            }
            if (timeString != null) {
                dayTimeText.append(timeString);
            }
            dayTime.setText(dayTimeText.toString());

            location.setText(currentEvent.getString("location"));

            Object priceObj = currentEvent.get("price");
            if (priceObj != null) {
                price.setText("$" + priceObj.toString());
            } else {
                price.setText("$0");
            }

            description.setText(currentEvent.getString("description"));

            List<String> entrants = (List<String>) currentEvent.get("entrants");
            int entrantCount = entrants != null ? entrants.size() : 0;

            Long limitLong = currentEvent.getLong("limit");
            String limitDisplay = limitLong != null ? String.valueOf(limitLong) : "?";
            entrantNumbers.setText("Current Entrants: " + entrantCount + "/" + limitDisplay + " slots");

            Long sampleSizeLong = currentEvent.getLong("sampleSize");
            int sampleSize = sampleSizeLong != null ? sampleSizeLong.intValue() : 0;
            lotteryWinners.setText("Lottery Winners: " + sampleSize);

            // Decode event poster image, if present
            String image_base64 = currentEvent.getString("posterImage");
            if (image_base64 != null) {
                byte[] decodedBytes = Base64.decode(image_base64, Base64.DEFAULT);
                Bitmap image_bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                eventImageView.setImageBitmap(image_bitmap);
            }
        });
    }
}
