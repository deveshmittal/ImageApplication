package com.devesh.imageapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    ImageView imageView;
    BitmapCacheLoader bitmapCacheLoader;
    Button leftButton;
    Button rightButton;
    TextView tv;
    Handler h;
    int pos;
    WeakReference<FetchUrlTask> taskRef;
    List<WeakReference<FetchUrlTask>> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            pos = 0;
        } else {
            pos = savedInstanceState.getInt("count");
            urlCache = (LinkedHashMap<Integer, String>) savedInstanceState.getSerializable("urlCache");
        }
         taskList = new ArrayList<WeakReference<FetchUrlTask>>();
        fetchUrlforPosition(pos);
        bitmapCacheLoader = new BitmapCacheLoader();
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.img);
        imageView.setImageDrawable(new ColorDrawable(Color.BLACK));
        leftButton = (Button) findViewById(R.id.left);
        rightButton = (Button) findViewById(R.id.right);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);
        tv = (TextView) findViewById(R.id.tv);
        tv.setText(Integer.toString(pos));
        h = new Handler();

        // imagedownloader.download("http://media2.giga.de/2012/01/Android-logo.png",imageView);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("count", pos);
        outState.putSerializable("urlCache", urlCache);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left:
                if (pos >= 1)
                    pos = pos - 1;
                break;
            case R.id.right:
                pos = pos + 1;
                break;
            default:
                break;
        }
        if(taskList!=null && taskList.size()>0){
            for(WeakReference<FetchUrlTask> wr : taskList){
                if(wr.get()!=null && Math.abs(wr.get().getmPosition()-pos)>5 ){
                    wr.get().cancel(true);
                }
            }
        }

        tv.setText(Integer.toString(pos));
        imageView.setImageDrawable(new ColorDrawable(Color.BLACK));
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchUrlforPosition(pos);
            }
        }, 50);

    }

    private void fetchUrlforPosition(final int position) {
       final String url = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=android&start=" + position;
        if (getUrlFromCache(position) != null) {
            downLoadImage(getUrlFromCache(position), position);
        } else {
            h= new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    FetchUrlTask fetchUrlTask= new FetchUrlTask(position) ;
                    taskRef = new WeakReference<FetchUrlTask>(fetchUrlTask);
                    taskList.add(taskRef);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        fetchUrlTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
                    else
                        fetchUrlTask.execute(url);
                }
            },500);

        }
    }

    class FetchUrlTask extends AsyncTask<String, Void, String> {
        String url;
        int mPosition;

        FetchUrlTask(int position) {
            mPosition = position;
        }

        public int getmPosition() {
            return mPosition;
        }

        @Override
        protected String doInBackground(String... params) {
            url = params[0];
            if(isCancelled())
                return null;
            return googleUrlFetcher(url);
        }

        @Override
        protected void onPostExecute(String url) {


            super.onPostExecute(url);
            // Toast.makeText(MainActivity.this,"yo",Toast.LENGTH_LONG).show();


            downLoadImage(url, mPosition);


        }
    }

    private void downLoadImage(String url, int mPosition) {
        Toast.makeText(MainActivity.this, url + " mPosition: " + mPosition + "position: " + pos, Toast.LENGTH_LONG).show();
        if (url != null && mPosition == pos) {
            addUrlToCache(mPosition, url);
            if (bitmapCacheLoader != null) {
                bitmapCacheLoader.startDownload(url, imageView);
            }
        }
    }

    private String googleUrlFetcher(String url) {
        JSONObject responseData = JSONParser.getJSONFromUrl(url);
        String imageUrl = null;
        try {
            JSONObject result = null;
            if (responseData != null)
                result = responseData.getJSONObject("responseData");

            if (result != null
                    && result.getJSONArray("results") != null
                    && result.getJSONArray("results").length() > 0
                    && result.getJSONArray("results").getJSONObject(0).getString("unescapedUrl") != null
                    && result.getJSONArray("results").length() > 0
                    && result.getJSONArray("results").getJSONObject(0).getString("unescapedUrl").length() > 0) {
                imageUrl = result.getJSONArray("results").getJSONObject(0).getString("unescapedUrl");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return imageUrl;
    }

    //Hold 100 urls
    private HashMap<Integer, String> urlCache = new LinkedHashMap<Integer, String>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<Integer, String> eldest) {
            return (size() > 100);
        }
    };

    private void addUrlToCache(int position, String url) {
        if (url != null) {
            synchronized (urlCache) {
                urlCache.put(position, url);
            }
        }

    }

    private String getUrlFromCache(int position) {
        String url = urlCache.get(position);
        synchronized (urlCache) {
            if (url != null) {
                urlCache.remove(position);
                urlCache.put(position, url);

            }
        }
        return url;
    }

}
