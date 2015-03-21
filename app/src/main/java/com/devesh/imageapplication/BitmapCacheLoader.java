package com.devesh.imageapplication;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class BitmapCacheLoader {
    private final HashMap<String, Bitmap> bitmapCache =
            new LinkedHashMap<String, Bitmap>(4, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
                    if (size() > 4) {
                        softBitmapCache.put(eldest.getKey(),new SoftReference<Bitmap>(eldest.getValue()));
                        return true;
                    } else
                        return false;
                }
            };
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> softBitmapCache =
            new ConcurrentHashMap<String, SoftReference<Bitmap>>(12);

    public void startDownload(String url, ImageView imgView){
        Bitmap bitmap = getBitMapFromCache(url);
        if(bitmap!=null){
            stopBitmapDownloadTasksIfAny(url,imgView);
            imgView.setImageBitmap(bitmap);
        }else{
            downloadBitmap(url, imgView);
        }
   }

    private void downloadBitmap(String url, ImageView imgView) {
        if (url == null) {
            imgView.setImageDrawable(null);
            return;
        }
        if(stopBitmapDownloadTasksIfAny(url,imgView)){
            BitmapDownloaderTask task = new BitmapDownloaderTask(imgView);
            StoredDrawable downloadedDrawable = new StoredDrawable(task);
            imgView.setImageDrawable(downloadedDrawable);
            imgView.setMinimumHeight(200);
            task.execute(url);
        }
    }

    private boolean stopBitmapDownloadTasksIfAny(String url, ImageView imageView) {
            BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

            if (bitmapDownloaderTask != null) {
                String bitmapUrl = bitmapDownloaderTask.url;
                if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                    bitmapDownloaderTask.cancel(true);
                } else {
                    // The same URL is already being downloaded.
                    return false;
                }
            }
            return true;
        }


    private Bitmap getBitMapFromCache(String url) {
        synchronized (bitmapCache) {
            Bitmap bitmap = bitmapCache.get(url);
            if (bitmap != null) {
                bitmapCache.remove(url);
                bitmapCache.put(url, bitmap);
                return bitmap;
            }
        }
          SoftReference<Bitmap> softReference = softBitmapCache.get(url);
            if(softReference!=null){
                Bitmap bitmap = softReference.get();
                if(bitmap!=null){
                    return bitmap;
                }else{
                    softBitmapCache.remove(url);
                }
            }
        return null;

        }
    class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private String url;
        private final WeakReference<ImageView> ivRef;

        public BitmapDownloaderTask(ImageView iv) {
            ivRef = new WeakReference<ImageView>(iv);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            return Utils.downloadBitmapFromNetwork(url);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            addBitmapToCache(url, bitmap);
            if (ivRef != null) {
                ImageView imageView = ivRef.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                if ((this == bitmapDownloaderTask) ) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (bitmapCache) {
                bitmapCache.put(url, bitmap);
            }
        }
    }
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof StoredDrawable) {
                StoredDrawable downloadedDrawable = (StoredDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }
    static class StoredDrawable extends ColorDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapTaskReference;

        public StoredDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.BLACK);
            bitmapTaskReference =
                    new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapTaskReference.get();
        }
    }
 }

