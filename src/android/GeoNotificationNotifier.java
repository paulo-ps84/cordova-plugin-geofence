package com.cowbell.cordova.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class GeoNotificationNotifier {
    private NotificationManager notificationManager;
    private Context context;
    private BeepHelper beepHelper;
    private Logger logger;
    private NotificationChannel notificationChannel;

    public GeoNotificationNotifier(NotificationManager notificationManager, Context context) {
        this.notificationManager = notificationManager;
        this.context = context;
        this.beepHelper = new BeepHelper();
        this.logger = Logger.getLogger();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel("channelId", "channelName", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void notify(Notification notification, String transition) {
        notification.setContext(context);
        NotificationCompat.Builder mBuilder = null;

        logger.log(Log.DEBUG, notification.getDataJson());
        
        if(notification.data != null) {
            Date iniDate = null;
            Date endDate = null;
            String idCampanha = null;
            try {
                JSONObject json = new JSONObject(notification.getDataJson());
                String iniDateString = json.getString("iniDate");
                String endDateString = json.getString("endDate");
                idCampanha = json.getString("idCampanha");
                logger.log(Log.DEBUG, iniDateString);
                logger.log(Log.DEBUG, endDateString);
                logger.log(Log.DEBUG, idCampanha);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                iniDate = sdf.parse(iniDateString);
                endDate = sdf.parse(endDateString);
                logger.log(Log.DEBUG, iniDate.toString());
                logger.log(Log.DEBUG, endDate.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(iniDate != null && endDate != null) {
                if(endDate.compareTo(new Date()) < 0 ) {
                    logger.log(Log.DEBUG, "Data final da campanha < hoje, campanha finalizada, remover fence.");
                    removeGeofence(idCampanha, context);
                } else if (iniDate.compareTo(new Date()) > 0) {
                    logger.log(Log.DEBUG, "Data inicial da campanha > hoje, campanha futura, não disparar notificação.");

                } else if( (iniDate.compareTo(new Date()) <= 0) && (endDate.compareTo(new Date()) >= 0 ) ){
                    logger.log(Log.DEBUG, "Campanha esta dentro do intervalo de data, disparar notificação");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        mBuilder = new NotificationCompat.Builder(context, notificationChannel.getId());
                    } else {
                        mBuilder = new NotificationCompat.Builder(context);
                    }
                    mBuilder.setVibrate(notification.getVibrate())
                        .setSmallIcon(notification.getSmallIcon())
                        .setLargeIcon(notification.getLargeIcon())
                        .setAutoCancel(true)
                            .setContentTitle(notification.getTitle().replace("$transition", transition))
                        .setContentText(notification.getText());
            
                    if (notification.openAppOnClick) {
                        String packageName = context.getPackageName();
                        Intent resultIntent = context.getPackageManager()
                            .getLaunchIntentForPackage(packageName);
            
                        if (notification.data != null) {
                            resultIntent.putExtra("geofence.notification.data", notification.getDataJson());
                        }
            
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                            notification.id, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(resultPendingIntent);
                    }
                    try {
                        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(context, notificationSound);
                        r.play();
                    } catch (Exception e) {
                        beepHelper.startTone("beep_beep_beep");
                        e.printStackTrace();
                    }
                    notificationManager.notify(notification.id, mBuilder.build());
                    logger.log(Log.DEBUG, notification.toString());
                    //remover a fence após disparo da notificação
                    removeGeofence(idCampanha, context);
                } else {
                    logger.log(Log.DEBUG, "Não esta dentro do intervalo de data, não disparar notificação.");
                }
            }
        }   
    }

    private void removeGeofence(String id, Context context){
        GeoNotificationManager geoNotificationManager = new GeoNotificationManager(context);
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        geoNotificationManager.removeGeoNotifications(ids, null);
    }
}
