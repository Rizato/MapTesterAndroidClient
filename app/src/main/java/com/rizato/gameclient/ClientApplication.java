package com.rizato.gameclient;

import android.app.Application;

import com.rizato.gameclient.networking.NetworkHandlerThread;

/**
 * This is an application used to hold a reference to the active NetworkHandlerThread
 */
public class ClientApplication extends Application {
    public ClientApplication() {
        super();
    }

    private NetworkHandlerThread networkThread;

    public NetworkHandlerThread getNetworkThread() {
        return networkThread;
    }

    public void setNetworkThread(NetworkHandlerThread networkThread) {
        this.networkThread = networkThread;
    }
}
