package com.graphhopper.marmoset;

import fi.iki.elonen.SimpleWebServer;
import fi.iki.elonen.util.ServerRunner;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by alexander on 16/02/2016.
 */
public class Marmoset {

    private static MarmosetHopper mh;
    private static MarmosetSocketServer mss;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        mh = new MarmosetHopper();
        mh.init();

        startFileServer();
        startWebSocketServer();

        while (true) {
            mh.timestep();
            String data = mh.getVehicleData();
            mss.distributeData(data);
            Thread.sleep(1000);
        }
    }

    private static void startWebSocketServer()
    {
        int port = 8888;

        mss = new MarmosetSocketServer(new InetSocketAddress(port));
        mss.start();
        System.out.println("Listening for websocket requests.");
    }

    private static void startFileServer()
    {
        final File rootDir = new File("marmoset/src/main/webapp").getAbsoluteFile();
        new Thread() {
            @Override
            public void run() {
                ServerRunner.executeInstance(new SimpleWebServer(null, 8080, rootDir, true, null));
            }
        }.start();
    }


}
