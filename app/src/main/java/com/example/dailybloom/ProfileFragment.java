package com.example.dailybloom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileFragment extends Fragment {

    private CalendarView calendarView;
    private ProgressBar loadingIndicator;
    private FirebaseFirestore db;
    private String userId;
    private Map<String, DailyEntry> entriesMap = new HashMap<>();
    private List<EventDay> eventsList = new ArrayList<>();

    private BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshTodayEntry();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);

        IntentFilter filter = new IntentFilter("DATA_UPDATED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(dataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), dataUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }

        loadAllData();

        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar clickedDay = eventDay.getCalendar();
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(clickedDay.getTime());

                if (entriesMap.containsKey(dateStr)) {
                    DailyEntry entry = entriesMap.get(dateStr);
                    showQuoteDialog(entry);
                } else {
                    Toast.makeText(requireContext(),
                            "No entry for " + dateStr,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void loadAllData() {
        loadingIndicator.setVisibility(View.VISIBLE);
        calendarView.setVisibility(View.GONE);

        db = FirebaseFirestore.getInstance();
        userId = requireContext().getSharedPreferences("user_prefs", 0)
                .getString("user_id", null);

        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            loadingIndicator.setVisibility(View.GONE);
            return;
        }

        entriesMap.clear();
        eventsList.clear();

        loadTodayFromSharedPrefs();

        loadEntriesFromFirestore();
    }

    private void loadTodayFromSharedPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences("daily_data", 0);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());
        String quote = prefs.getString("daily_quote", null);
        String imageUrl = prefs.getString("daily_image_url", null);

        if (quote != null && imageUrl != null) {
            entriesMap.put(today, new DailyEntry(today, quote, imageUrl));
        }
    }

    private void loadEntriesFromFirestore() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());

        db.collection("users")
                .document(userId)
                .collection("daily_entries")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DailyEntry> entriesToLoad = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String date = document.getString("date");
                        String quote = document.getString("quote");
                        String imageUrl = document.getString("imageUrl");

                        DailyEntry entry = new DailyEntry(date, quote, imageUrl);
                        entriesMap.put(date, entry);

                        if (!date.equals(today) || !entriesMap.containsKey(today)) {
                            entriesToLoad.add(entry);
                        }
                    }

                    if (entriesMap.containsKey(today)) {
                        DailyEntry todayEntry = entriesMap.get(today);
                        if (!entriesToLoad.contains(todayEntry)) {
                            entriesToLoad.add(0, todayEntry);
                        }
                    }

                    if (entriesToLoad.isEmpty()) {
                        finishLoading();
                    } else {
                        loadAllImages(entriesToLoad);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to load entries",
                            Toast.LENGTH_SHORT).show();
                    finishLoading();
                });
    }

    private void loadAllImages(List<DailyEntry> entries) {
        AtomicInteger loadedCount = new AtomicInteger(0);
        int totalCount = entries.size();

        for (DailyEntry entry : entries) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(sdf.parse(entry.date));

                loadImageForDate(calendar, entry.imageUrl, () -> {
                    int loaded = loadedCount.incrementAndGet();
                    if (loaded == totalCount) {
                        finishLoading();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                int loaded = loadedCount.incrementAndGet();
                if (loaded == totalCount) {
                    finishLoading();
                }
            }
        }
    }

    private void loadImageForDate(Calendar calendar, String imageUrl, Runnable onComplete) {
        if (!isAdded()) {
            onComplete.run();
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .override(100, 100)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        EventDay event = new EventDay(calendar, drawable);
                        eventsList.add(event);
                        onComplete.run();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        onComplete.run();
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        EventDay event = new EventDay(calendar, R.drawable.offline_image);
                        eventsList.add(event);
                        onComplete.run();
                    }
                });
    }

    private void finishLoading() {
        if (!isAdded()) return;

        calendarView.setEvents(eventsList);

        loadingIndicator.setVisibility(View.GONE);
        calendarView.setVisibility(View.VISIBLE);
    }

    private void refreshTodayEntry() {
        loadAllData();
    }

    private void showQuoteDialog(DailyEntry entry) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.quote_image, null);

        ImageView imageView = dialogView.findViewById(R.id.image);
        TextView quoteText = dialogView.findViewById(R.id.quote);

        quoteText.setText(entry.quote);

        if (entry.imageUrl != null && !entry.imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(entry.imageUrl)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.offline_image);
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireContext().unregisterReceiver(dataUpdateReceiver);
    }

    private static class DailyEntry {
        String date;
        String quote;
        String imageUrl;

        DailyEntry(String date, String quote, String imageUrl) {
            this.date = date;
            this.quote = quote;
            this.imageUrl = imageUrl;
        }
    }
}