package com.jscheng.srich.image_loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import com.jscheng.srich.utils.MdUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Created By Chengjunsen on 2019/3/11
 */
public class NoteImageLoader implements IImagePoolListener {
    private static final String TAG = "NoteImageLoader";
    /**
     * 监听器
     */
    private List<NoteImageListener> mImageListeners;
    /**
     * 本地加载库
     */
    private LocalImagePool mLocalImagePool;
    /**
     * 网络加载库
     */
    private NetworkImagePool mHttpImagePool;
    /**
     * LrcCache
     */
    private NoteMemoryCache mMemoryCache;
    /**
     * DiskCache
     */
    private NoteDiskCache mDiskCache;

    private Handler mMainHandler;

    private static NoteImageLoader instance;

    public static NoteImageLoader with(Context context) {
        if (instance == null) {
            synchronized (NetworkImagePool.class) {
                if (instance == null) {
                    instance = new NoteImageLoader(context);
                }
            }
        }
        return instance;
    }

    private NoteImageLoader(Context context) {
        mImageListeners = new ArrayList<>();
        mMainHandler = new Handler(Looper.getMainLooper());
        mDiskCache = new NoteDiskCache(context);
        mMemoryCache = new NoteMemoryCache();

        mLocalImagePool = new LocalImagePool(mMemoryCache, mDiskCache, context.getApplicationContext(), this);
        mHttpImagePool = new NetworkImagePool(mMemoryCache, mDiskCache, context.getApplicationContext(), this);
    }

    public void loadBitmap(String url) {
        String key = getKeyFromUrl(url);
        if (isCacheBitmap(key)) {
            return;
        }
        asyncLoadBitmap(url, key);
    }

    public Bitmap getBitmap(String url, int maxWidth) {
        String key = getKeyFromUrl(url);
        Bitmap bitmap = getCacheBitmap(key, maxWidth);
        if (bitmap != null) {
            return bitmap;
        }
        asyncLoadBitmap(url, key);
        return null;
    }

    private Boolean isCacheBitmap(String key) {
        return mMemoryCache.isCache(key) && mDiskCache.isCache(key);
    }

    private Bitmap getCacheBitmap(String key, int maxWidth) {
        Bitmap bitmap = mMemoryCache.get(key);
        if (bitmap != null) {
            return bitmap;
        }
        bitmap = mDiskCache.get(key, maxWidth);
        if (bitmap != null) {
            return bitmap;
        }
        return null;
    }

    private void asyncLoadBitmap(String url, String key) {
        if (mLocalImagePool.isUrl(url)) {
            mLocalImagePool.submit(url, key);
        } else if (mHttpImagePool.isUrl(url)) {
            mHttpImagePool.submit(url, key);
        }
    }

    private String getKeyFromUrl(String url) {
        return MdUtil.encode(url);
    }

    public void addImageListener(NoteImageListener listener) {
        mImageListeners.add(listener);
    }

    public void removeImageListener(NoteImageListener listener) {
        mImageListeners.remove(listener);
    }

    @Override
    public void loadedSuccess(final String url, String key) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (NoteImageListener listener: mImageListeners) {
                    listener.onNoteImageSuccess(url);
                }
            }
        });
    }

    @Override
    public void loadedFailed(final String url, final String key, final String err) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (NoteImageListener listener : mImageListeners) {
                    listener.onNoteImageFailed(url, err);
                }
            }
        });
    }
}
