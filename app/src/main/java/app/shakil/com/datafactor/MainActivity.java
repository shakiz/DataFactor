package app.shakil.com.datafactor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView imageResolution,numberOfPixels,errorTXT,recognizedTXT;
    private FloatingActionButton floatingActionButtonCamera;
    private static int REQUEST_IMAGE_CAPTURE=1;
    private String timeStamp,fileName,currentPhotoPath;
    private File image,storageDirectory,photoFile;
    private ImageView imageViewForSavedImage;
    private ScrollView mainScrollLayout;
    private Bitmap imageBitmap;
    private int height,width,totalPixel;
    private int pixelAmount[];
    private FirebaseVisionImage firebaseVisionImage;
    private FirebaseVisionTextDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        //Setting the on click listener for floating camera button
        floatingActionButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });
    }
    //This method will be used to capture image with the default camera
    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
        else{
            Snackbar.make(mainScrollLayout, "Opening camera", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null){
                try{
                    photoFile=createImageFile();
                    displaytoastMessage(photoFile.getAbsolutePath());
                    Log.v("File directory:",""+photoFile.getAbsolutePath());
                    if (photoFile!=null){
                        Uri photoUri= FileProvider.getUriForFile(this,"app.shakil.com.datafactor.fileprovider",photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                        startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                    }
                }
                catch (Exception e){
                    displaytoastMessage(e.getMessage());
                    Log.v("Exception : ",""+e.getMessage());
                }
            }
        }
    }

    //This method will be used to create the image name with date and a default part with the extension and returns the location of path
    public File createImageFile(){
        timeStamp=new SimpleDateFormat("dd-MM-yyyyHHmmss").format(new Date());
        fileName="DataFactor_Shakil"+timeStamp;
        storageDirectory=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            image =File.createTempFile(fileName,".jpg",storageDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentPhotoPath=image.getAbsolutePath();
        return image;
    }
    //This method will be used to initialize all the attributes with xml
    public void init(){
        floatingActionButtonCamera = findViewById(R.id.fabCameraOpenXML);
        imageViewForSavedImage=findViewById(R.id.imageViewXML);
        imageResolution=findViewById(R.id.imageResolutionXML);
        numberOfPixels=findViewById(R.id.numberOfPixelsXML);
        mainScrollLayout=findViewById(R.id.mainScrollLayout);
        errorTXT=findViewById(R.id.errorTextsXML);
        recognizedTXT=findViewById(R.id.recognizedTextsXML);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This part will load the captured original image from the directory
        imageBitmap= BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        imageViewForSavedImage.setImageBitmap(imageBitmap);
        firebaseVisionImage=FirebaseVisionImage.fromBitmap(imageBitmap);
        detector=FirebaseVision.getInstance().getVisionTextDetector();
        pixelAmount=new int[]{imageBitmap.getHeight()*imageBitmap.getWidth()};
        for(int i=0;i<pixelAmount.length;i++){

            int pixel = pixelAmount[i];
            if(pixel!=0){
                totalPixel=totalPixel+pixelAmount[i];
            }

        }
        Log.d("BITMAP","PIXEL:"+totalPixel);
        //Setting the image resolution
        imageResolution.setText("Width : "+imageBitmap.getWidth()+"\nHeight :"+imageBitmap.getHeight());
        numberOfPixels.setText("Number of pixels : "+totalPixel);
        detector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                Toast.makeText(getApplicationContext(),"Task successful",Toast.LENGTH_LONG).show();
                processTxt(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"Task failed",Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processTxt(FirebaseVisionText text) {
        List<FirebaseVisionText.Block> blocks = text.getBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(MainActivity.this, "No Text :(", Toast.LENGTH_LONG).show();
            return;
        }
        for (FirebaseVisionText.Block block : text.getBlocks()) {
            String txt = block.getText();
            recognizedTXT.setTextSize(24);
            recognizedTXT.setText(txt);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==0){
            if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                captureImage();
            }
            else{
                displaytoastMessage("Please grant the permissions.");
            }
        }
    }
    //This method will be used to show any toast message
    private void displaytoastMessage(String message) {
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about_us) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
