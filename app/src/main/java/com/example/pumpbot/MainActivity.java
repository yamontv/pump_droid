package com.example.pumpbot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private String TAG = getClass().getName();
    public static final String PREFS_NAME = "app_pref";
    public static final String SKEY_NAME = "app_settings";
    public static final String SETTING_JSON = "com.example.pumpbot.SETTING_JSON";

    private EditText mAddress;
    private EditText mQuoteCurrency;
    private EditText mBalancePercent;
    private EditText mBuyPercentage;
    private EditText mSellPercentage;
    private EditText mPrepumpPercentage;
    private EditText mParser;
    private EditText mTgChannels;

    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddress = findViewById(R.id.editTextAddress);
        mQuoteCurrency = findViewById(R.id.editTextQuoteCurrency);
        mBalancePercent = findViewById(R.id.editTextBalancePercent);
        mBuyPercentage = findViewById(R.id.editTextBuyPercentage);
        mSellPercentage = findViewById(R.id.editTextSellPercentage);
        mPrepumpPercentage = findViewById(R.id.editTextPrepumpPercentage);
        mParser = findViewById(R.id.editTextParser);
        mTgChannels = findViewById(R.id.editTextTgChannels);

        mSharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        if (mSharedPrefs.contains(SKEY_NAME)) {
            loadSettingsFromJsonStr(mSharedPrefs.getString(SKEY_NAME, ""));
        }
    }

    /**
     * Called when the user taps the Connect button
     */
    public void connectServer(View view) {
        String set = createSettingsFromView();
        //save settings
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString(SKEY_NAME, set).commit();
        //run 2nd intent
        Intent intent = new Intent(this, BotInteractionActivity.class);
        intent.putExtra(SETTING_JSON, set);
        startActivity(intent);
    }

    private void loadSettingsFromJsonStr(String settings_json) {
        JSONObject obj;
        try {
            obj = new JSONObject(settings_json);
        } catch (Throwable t) {
            Log.e(TAG, "Could not parse malformed JSON");
            return;
        }

        try {
            if (!obj.get("cmd").equals("set"))
                return;
            Log.i(TAG, "l: " + settings_json);
            mAddress.setText(obj.getString("address"));
            mQuoteCurrency.setText(obj.getString("quote"));
            mBalancePercent.setText(obj.getString("quotep"));
            mBuyPercentage.setText(obj.getString("buyp"));
            mSellPercentage.setText(obj.getString("sellp"));
            mPrepumpPercentage.setText(obj.getString("prepumpp"));
            mParser.setText(obj.getString("parser"));
            mTgChannels.setText(obj.getString("channels"));

        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
        }
    }

    private String createSettingsFromView() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd", "set");
            obj.put("address", mAddress.getText().toString());
            obj.put("quote", mQuoteCurrency.getText().toString());
            obj.put("quotep", Float.parseFloat(mBalancePercent.getText().toString()));
            obj.put("buyp", Integer.parseInt(mBuyPercentage.getText().toString()));
            obj.put("sellp", Integer.parseInt(mSellPercentage.getText().toString()));
            obj.put("prepumpp", Integer.parseInt(mPrepumpPercentage.getText().toString()));
            obj.put("parser", mParser.getText().toString());
            obj.put("channels", mTgChannels.getText().toString());
        } catch (JSONException e) {
            Log.e(TAG, "unexpected JSON exception", e);
        }
        return obj.toString();
    }
}