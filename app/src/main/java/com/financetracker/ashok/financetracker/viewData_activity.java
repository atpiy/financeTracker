package com.financetracker.ashok.financetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;

public class viewData_activity extends Activity {

    String[] data;
    ListView listView;
    TextView thresholdtv;
    TextView totalAmounttv;


    String totalAmount = "";
    String threshold = "";
    Integer limit;

    String res = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        totalAmounttv = findViewById(R.id.totalAmt);
        thresholdtv = findViewById(R.id.thresholdAmt);

        if (Config.isNetworkStatusAvailable(getApplicationContext())) {
            new get_all_subject_specific_programs().execute();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    public class get_all_subject_specific_programs extends AsyncTask<Void, Void, Void> {

        Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new Dialog(viewData_activity.this);
            dialog.requestWindowFeature(getWindow().FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loader);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            String url = "http://192.168.43.10:3000/retrieveData";
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);
            Log.e(TAG, "URL : " + url);
            Log.e(TAG, "Got Response from url!");

            if(jsonStr.equals("Nothing")){
                res = "Nothing";
            }
            else if (!jsonStr.isEmpty()) {
                try {
                    JSONObject jsonData = new JSONObject(jsonStr);
                    JSONArray allData = jsonData.getJSONArray("allData");
                    totalAmount = jsonData.getString("totalAmount");
                    threshold = jsonData.getString("threshold");
                    limit = jsonData.getInt("limit");

                    int length = allData.length();
                    data = new String[length];

                    // looping through the Data
                    for (int i = 0; i < length; i++) {
                        JSONObject c = allData.getJSONObject(i);

                        data[i] = "Place : " + c.getString("place") + "\n";
                        data[i] += "Amount :" + c.getString("amount") + "\n";
                        data[i] +="Date : " + c.getString("purchase_date");
                        Log.e(TAG, "data[" + i + "] : "+data[i]);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Json parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Log.v(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Couldn't get json from server.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            dialog.dismiss();
            detailview();
        }
    }

    public void detailview() {
        if(res.equals("Nothing")){
            Toast.makeText(this, "Server is Down!", Toast.LENGTH_LONG).show();
        } else {
            listView = findViewById(R.id.listview);
            ArrayAdapter adapter = new ArrayAdapter<String>(viewData_activity.this, R.layout.activity_data_lv_singleitem, data);
            listView.setAdapter(adapter);

            totalAmounttv.setText(totalAmount,  TextView.BufferType.EDITABLE);
            thresholdtv.setText(threshold,  TextView.BufferType.EDITABLE);

            if(limit == 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.crossed_threshold_msg).setTitle(R.string.crossed_threshold_title);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        }
    }
}
