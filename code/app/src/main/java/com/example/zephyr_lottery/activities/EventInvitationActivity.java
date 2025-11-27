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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.zephyr_lottery.Event;
import com.example.zephyr_lottery.R;
import com.example.zephyr_lottery.repositories.EventRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class EventInvitationActivity extends AppCompatActivity {

    private EventRepository repo;
    private String event_code;
    private String user_email;

    private Button btnAccept;
    private Button btnDecline;

    private TextView title;
    private TextView closingDate;
    private TextView startEnd;
    private TextView dayTime;
    private TextView location;
    private TextView price;
    private TextView description;
    private ImageView eventImage;

    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private DocumentReference docRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_invitation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        event_code = getIntent().getStringExtra("EVENT_CODE");
        user_email = getIntent().getStringExtra("USER_EMAIL");

        btnAccept = findViewById(R.id.button_accept_invitation);
        btnDecline = findViewById(R.id.button_reject_invitation);

        title = findViewById(R.id.textView_title_invitation);
        closingDate = findViewById(R.id.textView_closingdate_invitation);
        startEnd = findViewById(R.id.textView_startenddates_invitation);
        dayTime = findViewById(R.id.textView_day_time_invitation);
        location = findViewById(R.id.textView_location_string_invitation);
        price = findViewById(R.id.textView_price_string_invitation);
        description = findViewById(R.id.textView_description_string_invitation);
        eventImage = findViewById(R.id.imageView_ent_eventImage_invitation);

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        docRef = eventsRef.document(event_code);

        loadEventDetails();

        btnAccept.setOnClickListener(v -> {

            //add users email to the accepted entrants.
            docRef.update("accepted_entrants", FieldValue.arrayUnion(user_email))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "event successfully accepted",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "event failed to accept?",
                                Toast.LENGTH_SHORT).show();
                    });

            //remove users email from the pending entrants (winners arraylist).
            docRef.update("winners", FieldValue.arrayRemove(user_email))
                    .addOnFailureListener(e -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "not removed from pending list?",
                                Toast.LENGTH_SHORT).show();
                    });

            //send user back to home screen
            finish();
        });

        btnDecline.setOnClickListener(v -> {

            //add my email to the rejected entrants
            docRef.update("rejected_entrants", FieldValue.arrayUnion(user_email))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "event successfully rejected",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "event failed to reject?",
                                Toast.LENGTH_SHORT).show();
                    });

            //remove users email from the pending entrants (winners arraylist).
            docRef.update("winners", FieldValue.arrayRemove(user_email))
                    .addOnFailureListener(e -> {
                        Toast.makeText(EventInvitationActivity.this,
                                "not removed from pending list?",
                                Toast.LENGTH_SHORT).show();
                    });

            //send user back to home sreen
            finish();
        });
    }

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

            //get image from database, convert to bitmap, display image.
            String image_base64 = currentEvent.getString("posterImage");
            if (image_base64 != null) {
                byte[] decodedBytes = Base64.decode(image_base64, Base64.DEFAULT);
                Bitmap image_bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                eventImage.setImageBitmap(image_bitmap);
            }
        });
    }
}