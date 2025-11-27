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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.zephyr_lottery.Event;
import com.example.zephyr_lottery.R;
import com.example.zephyr_lottery.repositories.EventRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;

public class OrgMyEventDetailsActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "OrgMyEventDetails";
    private final EventRepository repo = new EventRepository();

    private TextView detailsText;
    private ImageView eventImage;
    private Button backButton;
    private Button entrantsButton;
    private Button generateQrButton;
    private Button buttonDrawLottery;
    private Button editButton;

    private int eventCode;
    private String userEmail;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.org_my_event_details_activity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        eventCode = getIntent().getIntExtra("EVENT_CLICKED_CODE", -1);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        detailsText = findViewById(R.id.text_placeholder);
        backButton = findViewById(R.id.button_org_event_details_back);
        entrantsButton = findViewById(R.id.button_entrants);
        generateQrButton = findViewById(R.id.button_generate_qr);
        buttonDrawLottery = findViewById(R.id.button_draw_lottery);
        eventImage = findViewById(R.id.imageView_orgEventDetails);
        editButton = findViewById(R.id.button_org_event_details_edit);

        loadEventDetails();

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrgMyEventDetailsActivity.this, OrgMyEventsActivity.class);
            intent.putExtra("USER_EMAIL", userEmail);
            startActivity(intent);
        });

        entrantsButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrgMyEventDetailsActivity.this, OrgMyEventEntrantsActivity.class);
            intent.putExtra("EVENT_CLICKED_CODE", eventCode);
            intent.putExtra("USER_EMAIL", userEmail);
            startActivity(intent);
        });

        generateQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrgMyEventDetailsActivity.this, QRCodeOrgActivity.class);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("EVENT_CLICKED_CODE", eventCode);
            startActivity(intent);
        });

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrgMyEventDetailsActivity.this, OrgEditEventActivity.class);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("EVENT_CLICKED_CODE", eventCode);
            startActivity(intent);
        });

        buttonDrawLottery.setOnClickListener(v -> {
            ArrayList<String> winners = drawLotteryWinners();
            if (winners.isEmpty()) {
                Toast.makeText(this, "No entrants to draw from.", Toast.LENGTH_SHORT).show();
                return;
            }

            //save winners to database, send winners to dialogue
            event.setChosen_entrants(winners);
            db.collection("events").document(Integer.toString(eventCode))
                    .update("winners", winners)
                    .addOnSuccessListener(aVoid -> {
                        // Update each winner’s status to SELECTED
                        String eventIdStr = Integer.toString(eventCode);
                        for (String winnerId : winners) {
                            repo.updateParticipantStatus(eventIdStr, winnerId, "SELECTED");
                        }
                        // Optionally send invitations via notifications
                        repo.notifyAllSelectedEntrants(eventIdStr,
                                () -> Log.d(TAG, "Invitations sent to winners"),
                                e -> Log.e(TAG, "Failed to send invitations", e));

                        Toast.makeText(this, "Winners saved!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OrgMyEventDetailsActivity.this, DrawWinnersPopupActivity.class);
                        intent.putStringArrayListExtra("WINNERS", winners);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save winners", e);
                        Toast.makeText(this, "Failed to save winners", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadEventDetails() {
        if (eventCode == -1) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = Integer.toString(eventCode);
        DocumentReference docRef = db.collection("events").document(docId);
        docRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Event e = snapshot.toObject(Event.class);
                    if (e == null) {
                        Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    event = e;

                    String name = e.getName() != null ? e.getName() : "";
                    String time = e.getTime() != null ? e.getTime() : "";
                    String weekday = e.getWeekdayString() != null ? e.getWeekdayString() : "";
                    String location = e.getLocation() != null ? e.getLocation() : "";
                    String description = e.getDescription() != null ? e.getDescription() : "";
                    String period = e.getPeriod() != null ? e.getPeriod() : "";
                    float price = e.getPrice();
                    int limit = e.getLimit();
                    int sampleSize = e.getSampleSize();

                    //get image from class, convert to bitmap, display image.
                    String image_base64 = e.getPosterImage();
                    if (image_base64 != null) {
                        byte[] decodedBytes = Base64.decode(image_base64, Base64.DEFAULT);
                        Bitmap image_bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        eventImage.setImageBitmap(image_bitmap);
                    }

                    String text = ""
                            + "Name: " + name + "\n"
                            + "Time: " + time + " (" + weekday + ")\n"
                            + "Location: " + location + "\n"
                            + "Price: " + price + "\n"
                            + "Registration period: " + period + "\n"
                            + "Entrant limit: " + limit + "\n"
                            + "Sample size (lottery winners): " + sampleSize + "\n"
                            + "\nDescription:\n" + description;

                    detailsText.setText(text);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show()
                );
    }

    private ArrayList<String> drawLotteryWinners() {
        ArrayList<String> winners = new ArrayList<>();

        if (event == null) {
            Toast.makeText(this, "Event not loaded yet.", Toast.LENGTH_SHORT).show();
            return winners;
        }

        if (event.getEntrants() == null || event.getEntrants().isEmpty()) {
            return winners;
        }

        ArrayList<String> entrants = new ArrayList<>(event.getEntrants());
        int sampleSize = event.getSampleSize();

        if (sampleSize <= 0 || sampleSize > entrants.size()) {
            sampleSize = entrants.size();
        }

        Collections.shuffle(entrants);
        for (int i = 0; i < sampleSize; i++) {
            winners.add(entrants.get(i));
        }

        //tvWinners.setText("Last draw: " + winners.size() + " selected");

        return winners;
    }
}
