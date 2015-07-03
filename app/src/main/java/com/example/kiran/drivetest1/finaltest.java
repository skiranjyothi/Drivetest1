package com.example.kiran.drivetest1;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by kiran on 1/7/15.
 */
public class finaltest extends Activity {
    //Initialisation

    static final int 				REQUEST_ACCOUNT_PICKER = 1;
    static final int 				REQUEST_AUTHORIZATION = 2;
    static final int 				RESULT_STORE_FILE = 3;
    private static Uri mFileUri;
    private static Drive mService;
    private GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCredential=GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE)); // declaring a drive credential
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);    //to pick a account

        setContentView(R.layout.test5);
        final Button browse=(Button) findViewById(R.id.browse2);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            final Intent gallery=new Intent(Intent.ACTION_GET_CONTENT);          // to open file explorer
            mFileUri=Uri.parse(Environment.getExternalStorageDirectory().getPath()); // getting filepath to selected file
            gallery.setDataAndType(mFileUri,"*/*");
            startActivityForResult(gallery,RESULT_STORE_FILE);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {

            case REQUEST_ACCOUNT_PICKER:             //to pick an drive account
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        mService = getDriveService(mCredential);
                    }
                }
                break;

            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    //account already picked
                } else {
                    startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;

            case RESULT_STORE_FILE:
                mFileUri=data.getData();
                if(mFileUri!=null){

                    saveFileToDrive(data);
                }
                break;
        }

    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }  //to assign drive service to a service variable

    private void saveFileToDrive(final Intent data){
       toshare share=new toshare();
        share.execute(data);

    }

    public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class toshare extends AsyncTask<Intent,Void,Void>{

        Intent email;
        @Override
        protected Void doInBackground(Intent... params) {

            final Intent data=params[0];
            Thread t=new Thread(new Runnable() {
                @Override
                public void run() {

                    try{

                        mFileUri=data.getData();

                        ContentResolver cR=getApplicationContext().getContentResolver();

                        java.io.File fileContent = new java.io.File(mFileUri.getPath());
                        FileContent mediaContent = new FileContent(cR.getType(mFileUri), fileContent);

                        showToast("Selected " + mFileUri.getPath() + "to upload"); //to show which file was uploaded

                        File body = new File();
                        body.setTitle(fileContent.getName());
                        body.setMimeType(cR.getType(mFileUri));

                        com.google.api.services.drive.Drive.Files f1 = mService.files();
                        com.google.api.services.drive.Drive.Files.Insert i1 = f1.insert(body, mediaContent);
                        File file = i1.execute();   //uploading the file

                        if(file!=null){

                            showToast("Uploaded: " + file.getTitle());
                            String path=file.getId();
                            insertpermission(mService,path);  // to make the file public
                            email=sendemail(path);            // to start email activity
                            startActivity(email);

                        }


                    } catch (UserRecoverableAuthIOException e){
                        startActivityForResult(e.getIntent(),REQUEST_AUTHORIZATION);

                    } catch (IOException e) {
                        e.printStackTrace();
                        showToast("Transfer ERROR: " + e.toString());
                    }


                }
            });
            t.start();
            return null;
        }
    }

    private  static Permission insertpermission(Drive service, String fileid)  // method to make a file public
    {


        Permission newPermission1 = new Permission();
        newPermission1.setValue("me");
        newPermission1.setType("anyone");
        newPermission1.setRole("writer");
        try{

            return service.permissions().insert(fileid,newPermission1).execute();
        } catch (IOException e) {
            e.printStackTrace();

        }

        return null;

    }

    private static Intent sendemail(String fileid)  //method which returns an intent to send emails
    {
        Intent emailintent=new Intent(Intent.ACTION_SEND);
       // emailintent.putExtra(Intent.EXTRA_EMAIL, new String[]{"skiranjyothi21095@gmail.com"});
        emailintent.setType("text/html");
        String content="https://drive.google.com/file/d/"+fileid+"/edit?usp=docslist_api"; // creaating the link
        emailintent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(content));                   // adding the link to body
        return emailintent;

    }
}
