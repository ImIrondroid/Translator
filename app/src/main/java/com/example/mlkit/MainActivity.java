package com.example.mlkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView tvshow, resultText;
    private ImageView imageView;
    private Bitmap bitmap;
    private Bundle b;

    String transresult ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageview);
        tvshow = findViewById(R.id.tvshow);
        resultText = findViewById(R.id.resultText);

        if(savedInstanceState !=null) {
            Log.d("restart","restart");
            b = savedInstanceState.getBundle("b");
            Bitmap bmp = (Bitmap) b.get("data");
            imageView.setImageBitmap(bmp);
        }
    }

    public void captureImage(View view) {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {

            try {
                b = data.getExtras();
                bitmap = (Bitmap) b.get("data");
             //   bitmap = resizeBitmapImageFn(bitmap,350);
                imageView.setImageBitmap(bitmap);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void detectText(View view) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        recognizeText(image);

       /* FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        List<FirebaseVisionText.Block> blocks = firebaseVisionText.getBlocks();

                        if(blocks.size() == 0) {
                            Toast.makeText(MainActivity.this, "Nothing Found!!!", Toast.LENGTH_SHORT).show();
                        } else {
                            for(FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                                String text = block.getText();
                                tvshow.setText(text);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });*/
    }

    public Bitmap resizeBitmapImageFn(Bitmap bmpSource, int maxResolution)
    {
        int iWidth = bmpSource.getWidth();      //비트맵이미지의 넓이
        int iHeight = bmpSource.getHeight();     //비트맵이미지의 높이
        int newWidth = iWidth ;
        int newHeight = iHeight ;
        float rate = 0.0f;

        //이미지의 가로 세로 비율에 맞게 조절

        if(iWidth > iHeight ){
            if(maxResolution < iWidth ){
                rate = maxResolution / (float) iWidth ;
                newHeight = (int) (iHeight * rate);
                newWidth = maxResolution;
            }

        }else{
            if(maxResolution < iHeight ){
                rate = maxResolution / (float) iHeight ;
                newWidth = (int) (iWidth * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(
                bmpSource, newWidth, newHeight, true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("b",b);
    }

    private void recognizeText(FirebaseVisionImage image) {

        // [START get_detector_default]
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // [END get_detector_default]

        // [START run_detector]
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                // [START_EXCLUDE]
                                // [START get_text]
                                String result = "";
                                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                                    Rect boundingBox = block.getBoundingBox();
                                    Point[] cornerPoints = block.getCornerPoints();
                                    String text = block.getText();

                                    for (FirebaseVisionText.Line line: block.getLines()) {
                                        // ...
                                        for (FirebaseVisionText.Element element: line.getElements()) {
                                            // ...
                                            String word = element.getText();
                                            result+=word;

                                        }
                                        result+=" ";
                                    }
                                    result+="\n";
                                }
                                tvshow.setText(result);

                                new BackgroundTask().execute();

                                // [END get_text]
                                // [END_EXCLUDE]
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        // [END run_detector]
    }

    class BackgroundTask extends AsyncTask<Integer, Integer, Integer> {


        @Override
        protected Integer doInBackground(Integer... integers) {
            StringBuilder output = new StringBuilder();
            String clientId = "Your client Id";
            String clientSecret = "Your client Secret";

            try {
                String text = URLEncoder.encode(tvshow.getText().toString(), "UTF-8");
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";

                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id",clientId);
                con.setRequestProperty("X-Naver-Client-Secret",clientSecret);

                String postParams = "source=en&target=ko&text="+text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }

                String inputLine;
                while((inputLine = br.readLine()) != null ) {
                    output.append(inputLine);
                }
                br.close();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            transresult = output.toString();
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(transresult);
            if(element.getAsJsonObject().get("errorMessage") != null) {
                Log.e("번역 오류", "번역 오류가 발생했습니다. " + "[오류 코드 : " + element.getAsJsonObject().get("errorCode").getAsString() + "]");
            } else if(element.getAsJsonObject().get("message") != null){
                resultText.setText(element.getAsJsonObject().get("message").getAsJsonObject().get("result").getAsJsonObject().get("translatedText").getAsString());
            }
        }
    }
}
