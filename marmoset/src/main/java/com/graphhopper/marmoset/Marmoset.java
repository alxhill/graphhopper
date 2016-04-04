package com.graphhopper.marmoset;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

        if (args.length == 0 || args[0].equals("--web"))
        {
            startFileServer();
            startWebSocketServer();
        }
        else if (args[0].equals("--file"))
        {
            startFileSimulation(args[1], Integer.parseInt(args[2]));
        }

        System.out.println("Press enter to terminate");
        try
        {
            System.in.read();
        } catch (Throwable ignored) {}

        System.exit(0);
    }

    private static void startFileSimulation(String outfile, int initialVehicles) throws IOException
    {
        PrintWriter p = new PrintWriter("simulations/" + outfile, "UTF-8");
        start(initialVehicles);
        while (mh.getVehicleCount() > 0)
        {
            if (mh.timestep()) {
                logger.info("===ITERATION [" + iteration + "]===");
                if (iteration % 10 == 0)
                    logger.info(mh.getVehicleCount() + " vehicles remaining");
                String metrics = mh.getMetrics();
                p.println(metrics);
                iteration++;
            }
        }
        p.close();
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
        if (mh.timestep())
        {
            logger.info("===ITERATION [" + iteration + "]===");
            mh.getMetrics();
            iteration++;
            ByteBuffer data = mh.getVehicleBytes();
            mss.distributeData(data);
        }
        else
        {
            logger.info("Timestep failed, not iterating");
        }
    }
}
