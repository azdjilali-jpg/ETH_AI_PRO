package com.ethai.pro;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class MainActivity extends Activity {
    TextView status, price, signal, probs;
    Button refresh;
    Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUI();
        fetch();
    }

    void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,28,28,28);
        root.setBackgroundColor(Color.rgb(16,16,16));

        TextView title = new TextView(this);
        title.setText("ETH AI PRO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1,80));

        TextView pair = new TextView(this);
        pair.setText("ETHUSDT  •  5 MIN");
        pair.setTextColor(Color.LTGRAY);
        pair.setTextSize(16);
        pair.setGravity(Gravity.CENTER);
        root.addView(pair);

        price = make("السعر: —", 25);
        signal = make("الاتجاه: جارٍ التحليل...", 25);
        probs = make("الصعود: —    الهبوط: —", 19);
        status = make("الحالة: الاتصال بـ Binance...", 14);

        root.addView(price);
        root.addView(signal);
        root.addView(probs);

        refresh = new Button(this);
        refresh.setText("تحديث التوقع");
        refresh.setOnClickListener(v -> fetch());
        root.addView(refresh);

        root.addView(status);

        TextView note = make("وضع تجريبي: لا يتم تنفيذ أي تداول.", 14);
        note.setTextColor(Color.GRAY);
        root.addView(note);

        setContentView(root);
    }

    TextView make(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setPadding(0,22,0,22);
        return t;
    }

    void fetch() {
        status.setText("الحالة: تحميل آخر شموع ETHUSDT...");
        new Thread(() -> {
            try {
                URL u = new URL("https://data-api.binance.vision/api/v3/klines?symbol=ETHUSDT&interval=5m&limit=60");
                HttpURLConnection c = (HttpURLConnection)u.openConnection();
                c.setConnectTimeout(12000); c.setReadTimeout(12000);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder s = new StringBuilder(); String line;
                while((line=r.readLine())!=null) s.append(line);
                JSONArray a = new JSONArray(s.toString());
                JSONObject last = new JSONObject();
                double close = 0, open=0, high=0, low=0;
                for(int i=0;i<a.length();i++){
                    JSONArray k=a.getJSONArray(i);
                    open=k.getDouble(1); high=k.getDouble(2); low=k.getDouble(3); close=k.getDouble(4);
                }
                double change=(close-open)/open;
                double score=0.5 + Math.tanh(change*800.0)*0.12;
                score=Math.max(0.05,Math.min(0.95,score));
                double up=score*100, down=(1-score)*100;
                String dir=score>=0.5 ? "صعود" : "هبوط";
                final double finalClose = close; final double finalUp = up; final double finalDown = down; final String finalDir = dir;
 runOnUiThread(() -> {
                    price.setText(String.format("السعر: %.2f USDT", finalClose));
                    signal.setText("الاتجاه المتوقع: "+finalDir);
                    probs.setText(String.format("الصعود: %.2f%%    الهبوط: %.2f%%",finalUp,finalDown));
                    status.setText("الحالة: تم تحديث 60 شمعة من Binance");
                });
            } catch(Exception e) {
                runOnUiThread(() -> status.setText("خطأ في الاتصال: "+e.getClass().getSimpleName()));
            }
        }).start();
    }
}
