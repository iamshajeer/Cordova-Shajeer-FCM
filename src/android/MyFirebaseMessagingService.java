package com.shajeer.robosoft.plugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by Shajeer Ahamed on 08/06/2017.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMPlugin";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        FcmMessage fcmMessage = getFcmMessageData(remoteMessage);        
        new SendNotification(fcmMessage).execute();
    }

    private FcmMessage getFcmMessageData(RemoteMessage remoteMessage) {
        FcmMessage fcmMessage =null;

        if (remoteMessage != null) {
            fcmMessage = getNotificationPayloadData(remoteMessage);
        }
        return fcmMessage;
    }

    private FcmMessage getNotificationPayloadData(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        FcmMessage fcmMessage = new FcmMessage();

        if (data != null && data.size() > 0) {

            if (data.containsKey(FcmNotificationMessageConstants.KEY_IMAGE)) {
                String image = data.get(FcmNotificationMessageConstants.KEY_IMAGE);
                fcmMessage.setImage(image);
            }

            if (data.containsKey(FcmNotificationMessageConstants.KEY_TITLE)) {
                String title = data.get(FcmNotificationMessageConstants.KEY_TITLE);
                fcmMessage.setTitle(title);
            }

            if (data.containsKey(FcmNotificationMessageConstants.KEY_TEXT)) {
                String text = data.get(FcmNotificationMessageConstants.KEY_TEXT);
                fcmMessage.setText(text);
            }

            if (data.containsKey(FcmNotificationMessageConstants.KEY_BODY)) {
                String body = data.get(FcmNotificationMessageConstants.KEY_BODY);
                fcmMessage.setBody(body);
            }

            if (data.containsKey(FcmNotificationMessageConstants.KEY_CLICK_ACTION)) {
                String clickAction = data.get(FcmNotificationMessageConstants.KEY_CLICK_ACTION);
                fcmMessage.setClickAction(clickAction);
            }
        }
        return fcmMessage;
    }
    
    private interface FcmNotificationMessageConstants {
        String KEY_BODY = "body";
        String KEY_ICON = "icon";
        String KEY_CLICK_ACTION = "click_action";
        String KEY_IMAGE = "image";
        String KEY_TITLE = "title";
        String KEY_TEXT = "text";
    }

    private class FcmMessage {
        private String body;
        private String icon;
        private String clickAction;
        private String image;
        private String title;
        private String text;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getClickAction() {
            return clickAction;
        }

        public void setClickAction(String click_action) {
            this.clickAction = click_action;
        }
    }

    public class SendNotification extends AsyncTask<Void, Void, Bitmap> {
        private FcmMessage mFcmMessage;


        public SendNotification(FcmMessage fcmMessage) {
            mFcmMessage = fcmMessage;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            InputStream in;

            if (mFcmMessage != null) {
                String thumbnailUrl = mFcmMessage.getImage();

                if (!TextUtils.isEmpty(thumbnailUrl)) {

                    try {
                        URL url = new URL(thumbnailUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        in = connection.getInputStream();
                        return BitmapFactory.decodeStream(in);
                    } catch (MalformedURLException e) {
                        Log.e("FCM", e.getMessage());
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e("FCM", e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            sendNotification(mFcmMessage, result);
        }
    }

    /**
     * @param fcmMessage gcm message object
     * @param image      big thumbnail
     */
    private void sendNotification(FcmMessage fcmMessage, @Nullable Bitmap image) {
        Intent intent = new Intent(this, FCMPluginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        getTargetIntent(intent,fcmMessage);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String title = fcmMessage.getTitle();
        String body = fcmMessage.getBody();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(getNotificationIcon())
        .setContentTitle(!TextUtils.isEmpty(title) ? title : "")
        .setContentText(!TextUtils.isEmpty(body) ? body : "")
        .setAutoCancel(true)
        .setColor(Color.parseColor("#2377c5"))
        .setSound(defaultSoundUri)
        .setContentIntent(pendingIntent);

        if (image != null) {
            NotificationCompat.BigPictureStyle s = new NotificationCompat.BigPictureStyle()
            .bigPicture(image)
            .setSummaryText(body);
            notificationBuilder.setStyle(s);
        }else{
            Log.e("FCM","Image is null");
        }

        NotificationManager notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =  notificationBuilder.build();
        notificationManager.notify(0,notification);
    }

    private int getNotificationIcon() {
        int notificationIconId;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
             notificationIconId = getResources().getIdentifier("trans_icon", "drawable", getPackageName());
        } else {
            notificationIconId = getApplicationInfo().icon;
        }
        return notificationIconId;
    }

      private void getTargetIntent(Intent intent, FcmMessage fcmMessage) {
        intent.putExtra(FcmNotificationMessageConstants.KEY_BODY, fcmMessage.getBody());
        intent.putExtra(FcmNotificationMessageConstants.KEY_CLICK_ACTION, fcmMessage.getClickAction());
        intent.putExtra(FcmNotificationMessageConstants.KEY_ICON, fcmMessage.getIcon());
        intent.putExtra(FcmNotificationMessageConstants.KEY_IMAGE, fcmMessage.getImage());
        intent.putExtra(FcmNotificationMessageConstants.KEY_TEXT, fcmMessage.getText());
        intent.putExtra(FcmNotificationMessageConstants.KEY_TITLE, fcmMessage.getTitle());
    }
}