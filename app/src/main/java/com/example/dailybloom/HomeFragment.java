package com.example.dailybloom;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private View view;
    private ImageView image;
    private TextView text;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private ListenerRegistration dailyEntryListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        image = view.findViewById(R.id.image);
        text = view.findViewById(R.id.quote);

        prefs = requireActivity().getSharedPreferences("daily_data", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        setupDailyEntryListener();

        addTestButton();
        return view;
    }

    private void setupDailyEntryListener() {
        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", null);

        if (userId == null) {
            displayTodayQuoteAndImage();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        dailyEntryListener = db.collection("users")
                .document(userId)
                .collection("daily_entries")
                .document(today)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        displayTodayQuoteAndImage();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String quote = documentSnapshot.getString("quote");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        prefs.edit()
                                .putString("daily_quote", quote)
                                .putString("daily_image_url", imageUrl)
                                .putString("daily_date", today)
                                .commit();

                        updateUI(quote, imageUrl);

                        Intent intent = new Intent("DATA_UPDATED");
                        requireContext().sendBroadcast(intent);
                    } else {
                        displayTodayQuoteAndImage();
                    }
                });
    }

    private void updateUI(String quote, String imageUrl) {
        if (quote != null) {
            text.setText(quote);
        } else {
            text.setText("Your daily quote will appear here");
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(image);
        } else {
            image.setImageResource(R.drawable.offline_image);
        }
    }

    private void displayTodayQuoteAndImage() {
        String quote = prefs.getString("daily_quote", "Your daily quote will appear here");
        String imageUrl = prefs.getString("daily_image_url", null);
        updateUI(quote, imageUrl);
    }

    private void addTestButton() {
        Button testButton = view.findViewById(R.id.test_button);

        if (testButton != null) {
            testButton.setOnClickListener(v -> {
                MyApp app = (MyApp) requireActivity().getApplication();
                app.updateQuoteNow();

                Toast.makeText(requireContext(),
                        "Updating quote...",
                        Toast.LENGTH_SHORT).show();

                v.postDelayed(() -> {
                    displayTodayQuoteAndImage();

                    Intent intent = new Intent("DATA_UPDATED");
                    requireContext().sendBroadcast(intent);
                }, 3000);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dailyEntryListener != null) {
            dailyEntryListener.remove();
        }
    }
}