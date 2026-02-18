package com.example.dailybloom;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DailyQuoteWorker extends Worker {

    public DailyQuoteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("daily_data", Context.MODE_PRIVATE);

        SharedPreferences userPrefs = getApplicationContext()
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", null);

        if (userId == null) {
            return Result.retry();
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Result[] result = {Result.failure()};
        final CountDownLatch latch = new CountDownLatch(1);

        db.collection("quotes")
                .whereArrayContains("usedBy", userId)
                .get()
                .addOnSuccessListener(usedQuery -> {
                    db.collection("quotes")
                            .get()
                            .addOnSuccessListener(allQuotesQuery -> {
                                List<DocumentSnapshot> allQuotes = allQuotesQuery.getDocuments();
                                List<DocumentSnapshot> usedQuotes = usedQuery.getDocuments();

                                allQuotes.removeIf(quote -> {
                                    for (DocumentSnapshot used : usedQuotes) {
                                        if (used.getId().equals(quote.getId())) {
                                            return true;
                                        }
                                    }
                                    return false;
                                });

                                if (allQuotes.isEmpty()) {
                                    processQuotes(db, allQuotesQuery.getDocuments(), prefs, today, userId, result, latch);
                                } else {
                                    processQuotes(db, allQuotes, prefs, today, userId, result, latch);
                                }
                            })
                            .addOnFailureListener(e -> {
                                result[0] = Result.retry();
                                latch.countDown();
                            });
                })
                .addOnFailureListener(e -> {
                    result[0] = Result.retry();
                    latch.countDown();
                });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                return Result.retry();
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }

        return result[0];
    }

    private void processQuotes(FirebaseFirestore db, List<DocumentSnapshot> quotes,
                               SharedPreferences prefs, String today, String userId,
                               Result[] result, CountDownLatch latch) {
        if (quotes.isEmpty()) {
            result[0] = Result.retry();
            latch.countDown();
            return;
        }

        DocumentSnapshot selectedQuote = quotes.get(new Random().nextInt(quotes.size()));
        String quote = selectedQuote.getString("quote");
        String quoteId = selectedQuote.getId();

        db.collection("quotes").document(quoteId)
                .update("usedBy", FieldValue.arrayUnion(userId));

        db.collection("photos")
                .whereArrayContains("usedBy", userId)
                .get()
                .addOnSuccessListener(usedImagesQuery -> {
                    db.collection("photos")
                            .get()
                            .addOnSuccessListener(allImagesQuery -> {
                                List<DocumentSnapshot> allImages = allImagesQuery.getDocuments();
                                List<DocumentSnapshot> usedImages = usedImagesQuery.getDocuments();

                                allImages.removeIf(image -> {
                                    for (DocumentSnapshot used : usedImages) {
                                        if (used.getId().equals(image.getId())) {
                                            return true;
                                        }
                                    }
                                    return false;
                                });

                                if (allImages.isEmpty()) {
                                    processImages(db, allImagesQuery.getDocuments(), quote, quoteId,
                                            prefs, today, userId, result, latch);
                                } else {
                                    processImages(db, allImages, quote, quoteId, prefs, today, userId, result, latch);
                                }
                            })
                            .addOnFailureListener(e -> {
                                result[0] = Result.retry();
                                latch.countDown();
                            });
                })
                .addOnFailureListener(e -> {
                    result[0] = Result.retry();
                    latch.countDown();
                });
    }

    private void processImages(FirebaseFirestore db, List<DocumentSnapshot> images,
                               String quote, String quoteId, SharedPreferences prefs,
                               String today, String userId, Result[] result, CountDownLatch latch) {
        if (images.isEmpty()) {
            result[0] = Result.retry();
            latch.countDown();
            return;
        }

        DocumentSnapshot selectedImage = images.get(new Random().nextInt(images.size()));
        String imageUrl = selectedImage.getString("imageUrl");
        String imageId = selectedImage.getId();

        db.collection("photos").document(imageId)
                .update("usedBy", FieldValue.arrayUnion(userId));

        prefs.edit()
                .putString("daily_quote", quote)
                .putString("daily_image_url", imageUrl)
                .putString("daily_date", today)
                .putString("current_quote_id", quoteId)
                .putString("current_image_id", imageId)
                .commit();

        saveToUserCollection(db, userId, today, quote, imageUrl);

        updateWidgets(getApplicationContext());

        result[0] = Result.success();
        latch.countDown();
    }

    private void saveToUserCollection(FirebaseFirestore db, String userId, String date,
                                      String quote, String imageUrl) {
        db.collection("users")
                .document(userId)
                .collection("daily_entries")
                .document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("quote", quote);
                    entry.put("imageUrl", imageUrl);
                    entry.put("date", date);
                    entry.put("timestamp", new Date());
                    entry.put("isCustomized", false);

                    db.collection("users")
                            .document(userId)
                            .collection("daily_entries")
                            .document(date)
                            .set(entry);
                });
    }

    private void updateWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, FlowerWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                Intent updateIntent = new Intent(context, FlowerWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            // Handle error silently
        }
    }
}