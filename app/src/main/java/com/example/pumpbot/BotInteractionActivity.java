package com.example.pumpbot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class BotInteractionActivity extends AppCompatActivity {
    private JSONObject mSettings;
    private String mAddress;
    private TextView mTextView;

    private final String TAG = getClass().getName();
    private TcpClient mTcpClient;
    private boolean mIsConnected = false;

    private Button mActivateBtn;
    private Button mSell25Btn;
    private Button mSell50Btn;
    private Button mSell100Btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_interaction);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        try {
            mSettings = new JSONObject(intent.getStringExtra(MainActivity.SETTING_JSON));
        } catch (Throwable t) {
            Log.e(TAG, "Could not parse malformed JSON");
        }
        try {
            mAddress = mSettings.getString("address");
        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
        }
        mTextView = findViewById(R.id.textView);

        mActivateBtn = findViewById(R.id.buttonActivate);
        mSell25Btn = findViewById(R.id.buttonSell25);
        mSell50Btn = findViewById(R.id.buttonSell50);
        mSell100Btn = findViewById(R.id.buttonSell100);

        mActivateBtn.setEnabled(false);
        mSell25Btn.setEnabled(false);
        mSell50Btn.setEnabled(false);
        mSell100Btn.setEnabled(false);

        new ConnectTask().execute();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onBackPressed() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTcpClient.stopClient();
    }

    /**
     * Called when the user taps the sell25 button
     */
    public void sell25(View v) {
        sell_cmd(25, 1.0);
    }

    /**
     * Called when the user taps the sell50 button
     */
    public void sell50(View v) {
        sell_cmd(50, 1.0);
    }

    /**
     * Called when the user taps the sell100 button
     */
    public void sell100(View v) {
        sell_cmd(100, 1.0);
    }

    private void sell_cmd(int amount_percent, double sell_percent) {
        if (!mIsConnected)
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd", "sell");
            obj.put("percent", amount_percent);
            obj.put("sellp", sell_percent);
        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
            return;
        }

        mTcpClient.sendMessage(obj.toString());
    }

    /**
     * Called when the user taps the activate button
     */
    public void activate(View v) {
        if (!mIsConnected)
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd", "activate");
            obj.put("allow", ((CheckBox) findViewById(R.id.checkBoxAllow)).isChecked());
        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
            return;
        }

        mTcpClient.sendMessage(obj.toString());
    }

    private void displayResponse(JSONObject obj) {
        try {
            if (!obj.get("resp").equals("data"))
                return;

            // button color repr status
            if (obj.getBoolean("active")) {
                if (obj.getBoolean("allow"))
                    mActivateBtn.setBackgroundColor(Color.RED);
                else
                    mActivateBtn.setBackgroundColor(Color.GREEN);
            } else {
                mActivateBtn.setBackgroundColor(Color.BLUE);
                mSell25Btn.setEnabled(false);
                mSell50Btn.setEnabled(false);
                mSell100Btn.setEnabled(false);
            }

            String set = "<p>S:";
            set += " " + obj.getString("quote");
            set += " " + obj.getString("quotep");
            set += " " + obj.getString("buyp");
            set += " " + obj.getString("sellp");
            set += " " + obj.getString("prepumpp");
            set += " " + obj.getString("parser");
            set += " " + obj.getString("channels");
            set += "</p>";

            double quote_amnt_bckp = obj.getDouble("quote_amnt_bckp");
            double quote_amnt = obj.getDouble("quote_amnt");
            double percent;
            if (quote_amnt_bckp != 0)
                percent = (quote_amnt / quote_amnt_bckp - 1.0) * 100.0;
            else
                percent = 0;

            String bal = "<p>B:";
            bal += " " + String.format("%.8f", quote_amnt_bckp);
            bal += " " + String.format("%.8f", quote_amnt);
            bal += " " + String.format("%.1f", percent);
            bal += "</p>";

            double buy_price = obj.getDouble("buy_price");
            double ask_price = obj.getDouble("ask_price");
            double order_price = obj.getDouble("order_price");
            double amnt_start = obj.getDouble("amnt_start");
            double amnt_current = obj.getDouble("amnt_current");
            double order_amnt = obj.getDouble("order_amnt");

            if (obj.isNull("coin")) {
                mSell25Btn.setEnabled(false);
                mSell50Btn.setEnabled(false);
                mSell100Btn.setEnabled(false);

                mTextView.setText(Html.fromHtml(set + bal, Html.FROM_HTML_MODE_LEGACY));

                return;
            }

            mSell25Btn.setEnabled(true);
            mSell50Btn.setEnabled(true);
            mSell100Btn.setEnabled(true);

            String coin = "<h3>C: " + obj.getString("coin");
            coin += " " + String.format("%.8f", buy_price);
            coin += " " + String.format("%.8f", ask_price);
            coin += " " + amnt_current;
            coin += "</h3>";

            double price_percent = 0;
            double order_percent = 0;
            double amnt_percent = 0;
            double inorder_percent = 0;

            if (buy_price != 0) {
                price_percent = (ask_price / buy_price - 1.0) * 100.0;
                order_percent = (order_price / buy_price - 1.0) * 100.0;
            }

            if (amnt_start != 0) {
                amnt_percent = (amnt_current / amnt_start) * 100.0;
                inorder_percent = (order_amnt / amnt_start) * 100.0;
            }

            String stat = "<h1>";
            stat += price_percent > 0 ? "<font color='green'>" : "<font color='red'>";
            stat += String.format("%.1f", price_percent);
            stat += "  " + String.format("%.1f", order_percent);
            stat += "  " + String.format("%.1f", amnt_percent);
            stat += "  " + String.format("%.1f", inorder_percent);
            stat += "</font></h1>";

            mTextView.setText(Html.fromHtml(set + bal + coin + stat, Html.FROM_HTML_MODE_LEGACY));
        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
        }
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {
        @Override
        protected TcpClient doInBackground(String... message) {
            //we create a TCPClient object and
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(byte[] data, int len) {
                    publishProgress("response", new String(data, StandardCharsets.UTF_8));
                }
            });
            publishProgress("connecting");
            if (!mTcpClient.connect(mAddress)) {
                publishProgress("fail");
                return null;
            }
            publishProgress("connected");
            mTcpClient.sendMessage(mSettings.toString());
            mTcpClient.run();
            publishProgress("disconnected");
            mTcpClient = null;
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            switch (values[0]) {
                case "response":
                    JSONObject obj;
                    try {
                        obj = new JSONObject(values[1]);
                    } catch (Throwable t) {
                        Log.e(TAG, "Could not parse malformed JSON");
                        return;
                    }
                    displayResponse(obj);
                    break;
                case "connecting":
                    mTextView.setText(R.string.connecting);
                    break;
                case "connected":
                    mIsConnected = true;
                    mActivateBtn.setEnabled(true);
                    break;
                case "disconnected":
                case "fail":
                    mIsConnected = false;
                    finish();
                    break;
            }
        }
    }
}