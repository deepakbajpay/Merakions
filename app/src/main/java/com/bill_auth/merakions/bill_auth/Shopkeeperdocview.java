package com.bill_auth.merakions.bill_auth;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bill_auth.merakions.bill_auth.adapters.CustomListViewAdapter;
import com.bill_auth.merakions.bill_auth.beanclasses.BillItem;
import com.bill_auth.merakions.bill_auth.beanclasses.UserItem;
import com.bill_auth.merakions.bill_auth.services.RetrofitApiService;
import com.bill_auth.merakions.bill_auth.services.RetrofitServiceUtil;
import com.bill_auth.merakions.bill_auth.utils.Constants;
import com.bill_auth.merakions.bill_auth.utils.EncriptionHandeler;
import com.bill_auth.merakions.bill_auth.utils.Utilities;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Shopkeeperdocview extends AppCompatActivity implements CustomListViewAdapter.onCustomListViweAdapterItemClick {
    ListView listView;
    CustomListViewAdapter customListViewAdapter;
    private ArrayList<BillItem> billList;
    String uid;
    AVLoadingIndicatorView avl;
    private AsyncTask<Void, Integer, Boolean> downloadFileAsync;
    ProgressBar downloadProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shopkeeperdocview);
        billList = new ArrayList<>();
       
        avl = findViewById(R.id.avi_search);

        uid = Utilities.getUid(this);

        listView = findViewById(R.id.doclist);
        downloadProgressBar = findViewById(R.id.download_progress_bar);

        BillItem List1 = new BillItem("01", "11 Feb 2018", true);
        BillItem List2 = new BillItem("01", "23 Jun 2017", false);
        BillItem List3 = new BillItem("01", "23 Dec 2017", true);
        BillItem List4 = new BillItem("01", "1 Jan 2018", false);
        BillItem List5 = new BillItem("01", "15 Jan 2018", true);

        billList.add(List1);
        billList.add(List2);
        billList.add(List3);
        billList.add(List4);
        billList.add(List5);

        customListViewAdapter = new CustomListViewAdapter(this, billList, true, this);
        listView.setAdapter(customListViewAdapter);
        showAvi();
        fetchBills();
        hideAvi();
    }

    private void showAvi() {

        avl.smoothToShow();
    }


    private void hideAvi() {

        avl.smoothToHide();
    }

    @Override
    public void onAdapterItemClicked(int position) {
        downloadReportFromUrl(billList.get(position).getBillUrl(),"MyBill.jpg",false);
//        verifyBill(position);
    }

    private void verifyBill(int position) {

    }

    private void fetchBills() {
        billList.clear();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(Constants.CHILD_BILLS);
        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                System.out.println("Shopkeeperdocview.onChildAdded datasnapshot " + dataSnapshot);
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    System.out.println("Shopkeeperdocview.onChildAdded ds " + ds);
                    if (ds.getKey().equals(uid)) {
                        for (DataSnapshot ds1 : ds.getChildren()) {
                            System.out.println("Shopkeeperdocview.onChildAdded " + ds1);
                            BillItem billItem = ds1.getValue(BillItem.class);
                            billList.add(billItem);
                        }
                    }
                }

                customListViewAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //================++++++++++++++++++++++++++++++++++++++++=======================================

    private void downloadReportFromUrl(final String downloadUrl, final String fileName, final boolean hasShare) {

        final OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .build();
        Retrofit customRetrofit = new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://google.com")
                .build();
        RetrofitApiService service = RetrofitServiceUtil.createCustomService(customRetrofit, RetrofitApiService.class);


        downloadProgressBar.setVisibility(View.VISIBLE);
        service.downloadReport(downloadUrl).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {

                if (response.body() == null) {
//                    reportsLoadingIndicator.smoothToHide();
                    return;
                }

                if (response.isSuccessful()) {
                    if (isExternalStorageWritable()) {
                        String[] fileSplit = downloadUrl.split("\\.");
                        final String fileExt = fileSplit[fileSplit.length - 1];

                        downloadFileAsync = new AsyncTask<Void, Integer, Boolean>() {
                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                File createDownloadsDir = new File(Constants.DOWNLOAD_REPORTS_DIRECTORY_PATH);

                                File homeDirectory = new File(Constants.DOWNLOAD_REPORTS_DIRECTORY_PATH);
                                if (!homeDirectory.exists()) {
                                    boolean directoryCreated = createDownloadsDir.mkdirs();
                                }
                                try {
                                    File downloadedReport = new File(Constants.DOWNLOAD_REPORTS_DIRECTORY_PATH, fileName);
                                    InputStream inputStream = null;
                                    OutputStream outputStream = null;

                                    try {
                                        int read;
                                        byte[] buffer = new byte[1024*4];
                                        long total = 0;
                                        long fileSize = response.body().contentLength();
                                        long fileSizeDownloaded = 0;


                                        inputStream = response.body().byteStream();
                                        outputStream = new FileOutputStream(downloadedReport);

                                        while (true) {
                                            read = inputStream.read(buffer);
                                            if(read==-1)
                                                break;

                                            int progress = (int) ((total * 100) / fileSize);
                                            publishProgress(progress);
                                            outputStream.write(buffer, 0, read);

                                            total += read;
                                        }

                                        try {
                                            OutputStream os = EncriptionHandeler.decrypt("myKeyisThis",outputStream);
                                            outputStream.flush();
                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                        }


                                        return true;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return false;
                                    } finally {
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }

                                        if (outputStream != null) {
                                            outputStream.close();
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }

                            @Override
                            protected void onProgressUpdate(Integer... values) {
                                downloadProgressBar.setProgress(values[0]);
                            }

                            @Override
                            protected void onPostExecute(Boolean isSaved) {
                                super.onPostExecute(isSaved);
                                if (isSaved) {
                                    downloadProgressBar.setProgress(100);
                                    downloadProgressBar.setVisibility(View.GONE);
                                    downloadProgressBar.setProgress(0);
                                    if (!hasShare)
                                        Toast.makeText(Shopkeeperdocview.this, "file_downloaded_successfully", Toast.LENGTH_SHORT).show();
//                                    adapter.notifyDataSetChanged();
                                    if (hasShare) {
//                                        shareDownloadedFile(fileName, fileExt);
                                    }
                                } else {
                                    downloadProgressBar.setVisibility(View.GONE);
                                    System.out.println("ReportsFragment.onResponse Didnt Save");
                                }
                            }
                        };
                        downloadFileAsync.execute();


                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
