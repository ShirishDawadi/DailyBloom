package com.example.dailybloom;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

public class FlowerWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "FlowerWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences("daily_data", Context.MODE_PRIVATE);

        String quote = prefs.getString("daily_quote", "Your daily inspiration");
        String imageUrl = prefs.getString("daily_image_url", null);
        String date = prefs.getString("daily_date", "");


        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flower_widget_provider);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int widthPixels = (int) (minWidth * context.getResources().getDisplayMetrics().density);

        if (widthPixels <= 0) {
            widthPixels = 300;
        }

        Bitmap textBitmap = createTextBitmap(context, quote, widthPixels);
        views.setImageViewBitmap(R.id.widget_quote_text, textBitmap);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

                int targetWidth = Math.min(minWidth * 2, 800);
                int targetHeight = Math.min(minHeight * 2, 800);

                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(
                        context.getApplicationContext(),
                        R.id.widget_flower_image,
                        views,
                        appWidgetId
                );

                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .override(targetWidth, targetHeight)
                        .centerCrop()
                        .encodeFormat(Bitmap.CompressFormat.JPEG)
                        .encodeQuality(80)
                        .into(appWidgetTarget);

            } catch (Exception e) {
                views.setImageViewResource(R.id.widget_flower_image, R.drawable.offline_image);
            }
        } else {
            views.setImageViewResource(R.id.widget_flower_image, R.drawable.offline_image);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap createTextBitmap(Context context, String text, int width) {
        try {
            Typeface typeface = ResourcesCompat.getFont(context, R.font.dancing_script);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(typeface);
            paint.setTextSize(18 * context.getResources().getDisplayMetrics().scaledDensity);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);

            int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
            int maxTextWidth = width - (padding * 2);

            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();
            java.util.List<String> lines = new java.util.ArrayList<>();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float testWidth = paint.measureText(testLine);

                if (testWidth > maxTextWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            Rect bounds = new Rect();
            paint.getTextBounds("Ag", 0, 2, bounds);
            int lineHeight = bounds.height() ;
            int totalHeight = (lineHeight * lines.size()) + (padding * 2);

            Bitmap bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int backgroundColor = context.getResources().getColor(R.color.pink_3, null);
            canvas.drawColor(backgroundColor);

            float x = width / 2f;
            float y = padding - ((paint.descent() + paint.ascent()) / 2f);

            for (String line : lines) {
                canvas.drawText(line, x, y, paint);
                y += lineHeight;
            }

            return bitmap;

        } catch (Exception e) {

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(16 * context.getResources().getDisplayMetrics().scaledDensity);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);

            int padding = (int) (2 * context.getResources().getDisplayMetrics().density);
            int height = (int) (50 * context.getResources().getDisplayMetrics().density);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            int backgroundColor = context.getResources().getColor(R.color.pink_3, null);
            canvas.drawColor(backgroundColor);

            canvas.drawText(text, width / 2f, height / 2f, paint);

            return bitmap;
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        updateWidget(context, appWidgetManager, appWidgetId);
    }
}