package com.rishabh.github.instaimagedown.service;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.rishabh.github.instaimagedown.R;
import com.rishabh.github.instaimagedown.floatingbutton.DeleteActionActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;


public class CustomFloatingViewService extends Service implements FloatingViewListener {

    private static final String TAG = "CustomFloatingViewService";

    private static final int NOTIFICATION_ID = 908114;

    private IBinder mCustomFloatingViewServiceBinder;

    private FloatingViewManager mFloatingViewManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mFloatingViewManager != null) {
            return START_STICKY;
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mCustomFloatingViewServiceBinder = new CustomFloatingViewServiceBinder(this);
        final LayoutInflater inflater = LayoutInflater.from(this);
        final ImageView iconView = (ImageView) inflater.inflate(R.layout.widget_insta, null, false);
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String imageUrl=getURL();

                Pattern patt= Pattern.compile("https://www.instagram.com/p/.");
                Matcher matcher=patt.matcher(imageUrl);

                 if(matcher.find()){
                     Toast.makeText(getApplicationContext(),"Downloading started",Toast.LENGTH_LONG).show();
                     downloadImage(imageUrl);
                }else{
                     Toast.makeText(getApplicationContext(),"Wrong URL, copy sharable url",Toast.LENGTH_LONG).show();
                }


            }
        });

        mFloatingViewManager = new FloatingViewManager(this, this);
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_trash_fixed);
        mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
        // Setting Options(you can change options at any time)
        loadDynamicOptions();
        // Initial Setting Options (you can't change options after created.)
        final FloatingViewManager.Options options = loadOptions(metrics);

        mFloatingViewManager.addViewToWindow(iconView, options);

        startForeground(NOTIFICATION_ID, createNotification());

        return START_REDELIVER_INTENT;
    }

    private void downloadImage(String url) {

        DownloadManager.Request request = null;
        try {
            request = new DownloadManager.Request(Uri.parse(url));
        } catch (IllegalArgumentException e) {
        }
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setTitle("Instant Insta");
        request.setDescription("Downloading Image");

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                /* Try to determine the file extension from the url. Only allow image types. You
                 * can skip this check if you only plan to handle the downloaded file manually and
                 * don't care about file managers not recognizing the file as a known type */
//        String[] allowedTypes = {"png", "jpg", "jpeg", "gif", "webp"};
//        String suffix = url.substring(url.lastIndexOf(".") + 1).toLowerCase();
//        Toast.makeText(getApplicationContext(), "Invalid file type", Toast.LENGTH_LONG).show();

                /* set the destination path for this download */
          String name = "InstaImage" + Math.abs(new Random().nextInt());
//
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS +
//                File.separator + "InstagramImageDownloader" + File.separator + name + "." + suffix);
//        request.setDestinationInExternalPublicDir(Environment.getExternalStorageDirectory().getAbsolutePath(),
//                    File.separator + "InstagramImageDownloader" + File.separator + name);

            String uriString= "file://"+Environment.getExternalStorageDirectory().getAbsolutePath() +
                    File.separator + "image_test" + File.separator + name+ "." + "jpeg";
            Uri uri=Uri.parse(uriString);
            request.setDestinationUri(uri);

          /* allow the MediaScanner to scan the downloaded file */
            request.allowScanningByMediaScanner();
            final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                /* this is our unique download id */
            final long DL_ID = dm.enqueue(request);

                /* get notified when the download is complete */
            BroadcastReceiver mDLCompleteReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                        /* our download */
                    if (DL_ID == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)) {


                            /* get the path of the downloaded file */
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(DL_ID);
                        Cursor cursor = dm.query(query);
                        if (!cursor.moveToFirst()) {
                            Toast.makeText(getApplicationContext(), "Download error: cursor is empty", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                                != DownloadManager.STATUS_SUCCESSFUL) {
                            Toast.makeText(getApplicationContext(), "Download failed: no success status", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                        Toast.makeText(getApplicationContext(),"Image Downloaded",Toast.LENGTH_LONG).show();
                    }
                }
            };
                /* register receiver to listen for ACTION_DOWNLOAD_COMPLETE action */
            registerReceiver(mDLCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        }


    private String getURL() {
        ClipData data = ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip();
        if (data == null) {
            Toast.makeText(this, "nothing found to paste", Toast.LENGTH_SHORT).show();
        }
        ClipData.Item toPaste = data.getItemAt(0);
        String url = toPaste.getText().toString().replaceAll("\\s+", "") + "media/?size=l";
        if (url.length() < 6) {
            Toast.makeText(this, "url too short", Toast.LENGTH_SHORT).show();
            return null;
        }
        return toPaste.getText().toString().replaceAll("\\s+", "") +"media/?size=l";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mCustomFloatingViewServiceBinder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    /**
     * Viewを破棄します。
     */
    private void destroy() {
        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
    }

    /**
     * 通知を表示します。
     */
    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_insta_logo);
        builder.setContentTitle(getString(R.string.insta_content_title));
        builder.setContentText(getString(R.string.content_text));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        // PendingIntent作成
        final Intent notifyIntent = new Intent(this, DeleteActionActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }


    private void loadDynamicOptions() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final String displayModeSettings = sharedPref.getString("settings_display_mode", "FullScreen");
        if ("Always".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_SHOW_ALWAYS);
        } else if ("FullScreen".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
        } else if ("Hide".equals(displayModeSettings)) {
            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
        }

    }

    private FloatingViewManager.Options loadOptions(DisplayMetrics metrics) {
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Shape
        final String shapeSettings = sharedPref.getString("settings_shape", "Circle");
        if ("Circle".equals(shapeSettings)) {
            options.shape = FloatingViewManager.SHAPE_CIRCLE;
        } else if ("Rectangle".equals(shapeSettings)) {
            options.shape = FloatingViewManager.SHAPE_RECTANGLE;
        }

        // Margin
        final String marginSettings = sharedPref.getString("settings_margin", String.valueOf(options.overMargin));
        options.overMargin = Integer.parseInt(marginSettings);

        // MoveDirection
        final String moveDirectionSettings = sharedPref.getString("settings_move_direction", "Default");
        if ("Default".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_DEFAULT;
        } else if ("Left".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_LEFT;
        } else if ("Right".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_RIGHT;
        } else if ("Fix".equals(moveDirectionSettings)) {
            options.moveDirection = FloatingViewManager.MOVE_DIRECTION_NONE;
        }

        // Init X/Y
        final String initXSettings = sharedPref.getString("settings_init_x", "0");
        final String initYSettings = sharedPref.getString("settings_init_y", "1.0");
        if (!TextUtils.isEmpty(initXSettings) && !TextUtils.isEmpty(initYSettings)) {
            final int offset = (int) (48 + 8 * metrics.density);
            options.floatingViewX = (int) (metrics.widthPixels * Float.parseFloat(initXSettings) - offset);
            options.floatingViewY = (int) (metrics.heightPixels * Float.parseFloat(initYSettings) - offset);
        }

        // Initial Animation
        final boolean animationSettings = sharedPref.getBoolean("settings_animation", options.animateInitialMove);
        options.animateInitialMove = animationSettings;

        return options;
    }


    public static class CustomFloatingViewServiceBinder extends Binder {

        private final WeakReference<CustomFloatingViewService> mService;

        CustomFloatingViewServiceBinder(CustomFloatingViewService service) {
            mService = new WeakReference<>(service);
        }

        public CustomFloatingViewService getService() {
            return mService.get();
        }
    }

}
