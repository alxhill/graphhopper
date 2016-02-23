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
    private static boolean isRunning = false;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        mh = new MarmosetHopper();
        mh.init();

        startFileServer();
        startWebSocketServer();
    }

    public static void run()
    {
        if (!isRunning)
        {
            isRunning = true;
            new Thread() {
                @Override
                public void run()
                {
                    int i = 0;
                    while (true)
                    {
                        System.out.println("Running iteration " + i);
                        i++;
                        mh.timestep();
                        String data = mh.getVehicleData();
                        mss.distributeData(data);
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
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
