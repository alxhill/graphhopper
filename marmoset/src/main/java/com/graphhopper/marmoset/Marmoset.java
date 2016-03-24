package com.graphhopper.marmoset;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * Created by alexander on 16/02/2016.
 */
public class Marmoset {

    private static MarmosetHopper mh;
    private static MarmosetSocketServer mss;
    private static NanoHTTPD fileServer;
    private static boolean isRunning = false;

    private static Logger logger = LoggerFactory.getLogger(Marmoset.class);

    private static int iteration;


    public static void main(String[] args) throws IOException, InterruptedException
    {
        mh = new MarmosetHopper();
        mh.init();

        startFileServer();
        startWebSocketServer();

        System.out.println("Press enter to terminate");
        try
        {
            System.in.read();
        } catch (Throwable ignored) {}

        System.exit(0);
    }

    public static void start(int initialVehicles)
    {
        if (!isRunning)
        {
            isRunning = true;
            iteration = 0;
            mh.startSimulation(initialVehicles);
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

    public static void addVehicles(int count)
    {
        logger.info("Adding " + count + " vehicles (" + (count + mh.getVehicleCount()) + ")");
        IntStream.range(0, count).forEach(i -> mh.addVehicle());
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
        File rootDir = new File("marmoset/src/main/webapp").getAbsoluteFile();
        fileServer = new SimpleWebServer(null, 8080, rootDir, true, null);
        try
        {
            fileServer.start(5000, false);
            System.out.println("File server started");
        }
        catch (IOException e)
        {
            logger.error("Failed to start server: " + e);
        }
    }

    public static void nextTimestep() {
        logger.info("===ITERATION [" + iteration + "]===");
        iteration++;
        mh.timestep();
        ByteBuffer data = mh.getVehicleBytes();
        mss.distributeData(data);
    }
}
