package com.seapip.thomas.line_watchface.complications_polyfill;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.IComplicationManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ComplicationManager extends android.support.wearable.complications.ComplicationManager {
    private Context context;
    private CanvasWatchFaceService.Engine engine;
    private int[] watchFaceComplicationIds;
    private ComplicationProviderService[] providerServices;
    private PackageManager packageManager;

    public ComplicationManager(IComplicationManager service) {
        super(service);
    }

    public ComplicationManager(Context context, CanvasWatchFaceService.Engine engine) {
        super(null);
        this.context = context;
        this.engine = engine;
    }

    public void updateComplicationData(int id, ComplicationData complicationData) {
        engine.onComplicationDataUpdate(id, complicationData);
    }

    public void setActiveComplications(int... watchFaceComplicationIds) {
        this.watchFaceComplicationIds = watchFaceComplicationIds;
        providerServices = new ComplicationProviderService[watchFaceComplicationIds.length];
        try {
            int flags = PackageManager.GET_ACTIVITIES
                    | PackageManager.GET_CONFIGURATIONS
                    | PackageManager.GET_DISABLED_COMPONENTS
                    | PackageManager.GET_GIDS | PackageManager.GET_INSTRUMENTATION
                    | PackageManager.GET_INTENT_FILTERS
                    | PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS
                    | PackageManager.GET_RECEIVERS | PackageManager.GET_SERVICES
                    | PackageManager.GET_SIGNATURES;
            List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(flags);
            ArrayList<String> services = new ArrayList<>();
            for (PackageInfo packageInfo : packageInfos) {
                if (packageInfo.services != null) {
                    for (ServiceInfo serviceInfo : packageInfo.services) {
                        if (serviceInfo.permission != null && serviceInfo.permission.equals("com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER")) {
                            services.add(serviceInfo.name);
                            Log.d("LINE_SERVICE", serviceInfo.name);
                        }
                    }
                }
            }

            /*
            Get the first found complication provider and use it, assuming it's the only installed
            complication provider, if not change the idnex or just pass the name of teh class as a string.
            */
            Class providerService = Class.forName(services.get(0));
            providerServices[1] = (ComplicationProviderService) providerService.newInstance();
            Bundle metaData = context.getPackageManager().getServiceInfo(new ComponentName(context, providerService), PackageManager.GET_META_DATA).metaData;
            //Supported types of complication provider
            Log.d("LINE_META", metaData.getString("android.support.wearable.complications.SUPPORTED_TYPES"));
            //Update period of complication provider
            Log.d("LINE_META", String.valueOf(metaData.getInt("android.support.wearable.complications.UPDATE_PERIOD_SECONDS")));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | PackageManager.NameNotFoundException e) {
        }
        for (int id : watchFaceComplicationIds) {
            if (providerServices[id] != null) {
                providerServices[id].onComplicationActivated(id, ComplicationData.TYPE_SHORT_TEXT, this);
            }
        }
    }

    public void update() {
        //TODO: Check update period information and only call update method based on this interval.
        for (int id : watchFaceComplicationIds) {
            if (providerServices[id] != null) {
                providerServices[id].onComplicationUpdate(id, ComplicationData.TYPE_SHORT_TEXT, this);
            }
        }
    }
}
