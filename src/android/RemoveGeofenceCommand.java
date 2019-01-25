package com.cowbell.cordova.geofence;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.List;

public class RemoveGeofenceCommand extends AbstractGoogleServiceCommand implements OnCompleteListener<Void> {
    private List<String> geofencesIds;

    public RemoveGeofenceCommand(Context context, List<String> geofencesIds) {
        super(context);
        this.geofencesIds = geofencesIds;
    }

    @Override
    protected void ExecuteCustomCode() {
        if (geofencesIds != null && geofencesIds.size() > 0) {
            logger.log(Log.DEBUG, "Removing geofences...");
            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this.context);
            geofencingClient.removeGeofences(geofencesIds).addOnCompleteListener(this);
        } else {
            logger.log(Log.DEBUG, "Tried to remove Geofences when there were none");
            CommandExecuted();
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            logger.log(Log.DEBUG, "Geofences successfully removed");
            CommandExecuted();
        } else {
            logger.log(Log.DEBUG, "Geofences error on remove");
            CommandExecuted();
        }
    }
}
