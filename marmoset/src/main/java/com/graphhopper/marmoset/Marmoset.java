package com.graphhopper.marmoset;

import fi.iki.elonen.SimpleWebServer;
import fi.iki.elonen.util.ServerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(Marmoset.class);

    public static void main(String[] args) throws IOException, InterruptedException
    {
        mh = new MarmosetHopper();
        mh.init();

        startFileServer();
        startWebSocketServer();
    }

    public static void run(int initialVehicles)
    {
        if (!isRunning)
        {
            isRunning = true;
            Runnable task = () -> {
                int i = 0;
                mh.startSimulation(initialVehicles);
                while (true)
                {
                    if (!mh.paused())
                    {
                        logger.info("===ITERATION [" + i + "]===");
                        i++;
                        mh.timestep();
                        String data = mh.getVehicleData();
                        mss.distributeData(data);
                    }
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            new Thread(task).start();
        }
        else if (mh.paused())
        {
            mh.unpause();
        }
    }

    public static void pause()
    {
        mh.pause();
    }

    public static void addVehicle()
    {
        mh.addVehicle();
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
