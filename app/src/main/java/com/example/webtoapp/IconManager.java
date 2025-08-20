package com.example.webtoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.Random;

public class IconManager {
    private Context context;
    private Random random;

    public IconManager(Context context) {
        this.context = context;
        this.random = new Random();
    }

    public Bitmap generateTextIcon(String text) {
        int size = 192;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Generate random background color
        int[] colors = {
                Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
                Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F")
        };

        int backgroundColor = colors[random.nextInt(colors.length)];

        // Draw background circle
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setAntiAlias(true);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        // Draw text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(size * 0.4f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        String letter = text.substring(0, 1).toUpperCase();

        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

        float x = size / 2f;
        float y = size / 2f + textBounds.height() / 2f;

        canvas.drawText(letter, x, y, textPaint);

        return bitmap;
    }

    public Bitmap getDefaultIcon() {
        int size = 192;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw gradient background
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#667eea"));
        paint.setAntiAlias(true);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        // Draw web icon
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);

        // Draw globe lines
        canvas.drawCircle(radius, radius, radius * 0.6f, paint);
        canvas.drawLine(radius, radius * 0.4f, radius, radius * 1.6f, paint);
        canvas.drawLine(radius * 0.4f, radius, radius * 1.6f, radius, paint);

        // Draw curved lines
        RectF oval = new RectF(radius * 0.4f, radius * 0.6f, radius * 1.6f, radius * 1.4f);
        canvas.drawArc(oval, 0, 180, false, paint);
        canvas.drawArc(oval, 180, 180, false, paint);

        return bitmap;
    }

    public Bitmap resizeIcon(Bitmap original, int width, int height) {
        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    public Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }

    public Bitmap addRoundedCorners(Bitmap bitmap, float radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawRoundRect(rect, radius, radius, paint);

        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }
}