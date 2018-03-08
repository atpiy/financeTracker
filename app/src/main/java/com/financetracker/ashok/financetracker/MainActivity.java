package com.financetracker.ashok.financetracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String FILE_NAME = "temp.jpg";
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    ImageView imageView;
    String placestr = "";
    String amountstr = "";
    String purdatestr = "";
    TextView carname;
    TextView amount;
    TextView purchaseDate;
    Bitmap bitmap = null;
    String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ScrollView scroll = findViewById(R.id.myscroll);
        scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        scroll.setFocusable(true);
        scroll.setFocusableInTouchMode(true);
        scroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        });

        imageView = findViewById(R.id.imageView);
        TextView heading = findViewById(R.id.heading);
        Button click = findViewById(R.id.click);
        Button analyze = findViewById(R.id.analyze);
        Button save = findViewById(R.id.save);
        Button view = findViewById(R.id.view);
        carname = findViewById(R.id.place);
        amount = findViewById(R.id.amount);
        purchaseDate = findViewById(R.id.dttime);

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        purdatestr = formatter.format(date).toString();
        //Log.e(TAG, "vnk : " + formatter.format(date).toString());


        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage(R.string.dialog_select_prompt)
                        .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGalleryChooser();
                            }
                        })
                        .setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCamera();
                            }
                        });
                builder.create().show();
            }
        });

        analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bitmap == null)
                    Toast.makeText(MainActivity.this, "Image is empty",Toast.LENGTH_SHORT).show();
                else {
                    if (Config.isNetworkStatusAvailable(getApplicationContext())) {
                        new getCarNumber().execute();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //finish();
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }

                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean a = false;
                if (Config.isNetworkStatusAvailable(getApplicationContext())) {
                    if(carname.getText().toString().equals("") || amount.getText().toString().equals("") || purchaseDate.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, "TextFields cannot be empty",Toast.LENGTH_SHORT).show();
                    } else{
                        String amt = amount.getText().toString();
                        a = Pattern.matches("[0-9]+",amt);
                        if(!a) { amount.setError("Only Numbers are Allowed!");}
                    }

                    if(a) {
                        new saveTheData(MainActivity.this).execute();
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //finish();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }

            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, viewData_activity.class);
                startActivity(i);
            }
        });
    }

    public void startGalleryChooser() {
        if (permissionUtils.requestPermission(MainActivity.this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),GALLERY_IMAGE_REQUEST);
        } else {
            Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    public void startCamera() {
        Log.e(TAG, "In start Camera function.");
        if (permissionUtils.requestPermission(MainActivity.this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        } else {
            Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                bitmap = scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(), uri),1200);

                //callCloudVision(bitmap);
                imageView.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, "Image Picker Error", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, "Image picker gave us a null image.", Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public class getCarNumber extends AsyncTask<Void, Void, Void> {
        Dialog dialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Toast.makeText(getApplicationContext(), "Json Data is downloading", Toast.LENGTH_LONG).show();
            dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(getWindow().FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loader);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            String jsonstr="";
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            String img = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            //Log.e(TAG, "Base 64 Encoding: " + img);

            try {
                String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(img, "UTF-8");

                URL url = new URL("http://192.168.43.6:3000/process");
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();

                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    jsonstr += line;
                    System.out.println(line);
                }
                wr.close();
                rd.close();

            }catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException: " + e.getMessage());
            } catch (ProtocolException e) {
                Log.e(TAG, "ProtocolException: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }

            Log.e(TAG, "Result" + jsonstr);
            if ((!jsonstr.isEmpty())) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonstr);
                    placestr = jsonObj.getString("place");
                    amountstr = jsonObj.getString("amount");
                    //purdatestr = jsonObj.getString("final_date");

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
                Log.v(TAG, "Couldn't get Data(json) from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Couldn't get json from server.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(placestr.isEmpty() || amountstr.isEmpty() || purdatestr.isEmpty())
                Toast.makeText(getApplicationContext(), "Unable to process.", Toast.LENGTH_LONG).show();
            else {
                carname.setText(placestr,  TextView.BufferType.EDITABLE);
                amount.setText(amountstr,  TextView.BufferType.EDITABLE);
                purchaseDate.setText(purdatestr,  TextView.BufferType.EDITABLE);
            }
            dialog.dismiss();
        }
    }

    private class saveTheData extends AsyncTask<Void, Void, Void> {
        private Context context;
        Dialog dialog;

        saveTheData(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(getWindow().FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loader);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            String one = carname.getText().toString();
            String two = amount.getText().toString();
            String three = purchaseDate.getText().toString();

            String url = "http://192.168.43.6:3000/insertData?place="+one+"&cost="+two+"&dt="+three;
            Log.e(TAG, "Request sent: " + url);
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            try{
                if (!(jsonStr.isEmpty())) {
                    result = jsonStr.trim();
                }
            } catch(Exception e) {
                //Toast.makeText(loginActivity.this, "Oops! Server is Down!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "In Catch!");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Oops! Server is Down.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result1) {
            super.onPostExecute(result1);
            dialog.dismiss();
            if(result.equals("Nothing"))
                result = "Server is Down!";
            Toast.makeText(this.context, result, Toast.LENGTH_LONG).show();
        }
    }
}
