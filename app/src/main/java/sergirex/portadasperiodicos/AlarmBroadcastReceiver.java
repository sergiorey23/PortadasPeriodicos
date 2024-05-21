package sergirex.portadasperiodicos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);

    }

    void showNotification(Context context) {
        String CHANNEL_ID = "26081995";// The id of the channel.
        CharSequence name = context.getResources().getString(R.string.app_name);// The user-visible name of the channel.
        NotificationCompat.Builder mBuilder;
        Intent notificationIntent = new Intent(context, Portadas.class);
        Bundle bundle = new Bundle();
        notificationIntent.putExtras(bundle);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(mChannel);

        }
        mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.news_icon)
                .setLights(Color.CYAN, 300, 300)
                .setChannelId(CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_name));

        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setContentText(context.getString(R.string.notification_description));
        mBuilder.setAutoCancel(true);
        mNotificationManager.notify(1, mBuilder.build());
    }
}
