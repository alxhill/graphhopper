package com.graphhopper.marmoset;

import com.graphhopper.marmoset.event.EventManager;
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
        EventManager.trigger("init:start");
        mh = new MarmosetHopper();
        mh.init();

        if (args.length == 0 || args[0].equals("--web"))
        {
            startFileServer();
            startWebSocketServer();
        } else if (args[0].equals("--file")) {
            runOfflineSimulation(args[1], Integer.parseInt(args[2]));
            logger.info("Simulation complete.");
            return;
        }

        System.out.println("Press enter to terminate");
        EventManager.trigger("init:end");
        try
        {
            System.in.read();
        } catch (Throwable ignored) {}

        System.exit(0);
    }

    private static void runOfflineSimulation(String outfile, int initialVehicles) throws IOException
    {
        PrintWriter p = new PrintWriter("simulations/" + outfile, "UTF-8");
        p.println(MarmosetHopper.Metrics.getHeader());
        start(initialVehicles);
        while (mh.getVehicleCount() > 0 && mh.timestep(false))
        {
            EventManager.trigger("timestep:start", iteration);
            EventManager.trigger("timestep:end", iteration);
            logger.info("===ITERATION [" + iteration + "] VEHICLES [" + mh.getVehicleCount() + "]===");
            MarmosetHopper.Metrics metrics = mh.getMetrics();
            if (metrics == null)
                continue;
            logger.info(metrics.getDescription());
            p.println(metrics.toString());
            p.flush();

            if (iteration % 1000 == 0)
            {
                PrintWriter iterPrint = new PrintWriter("simulations/" + (outfile + iteration), "UTF-8");
                iterPrint.println(mh.getVehicleString());
                iterPrint.close();
            }

            iteration++;
        }
        p.close();
    }

    public static void start(int initialVehicles)
    {
        if (!isRunning)
        {
            isRunning = true;
            iteration = 0;
            EventManager.trigger("start", initialVehicles);
            mh.startSimulation(initialVehicles);
        }
        else if (mh.paused())
        {
            EventManager.trigger("unpause");
            mh.unpause();
        }
    }

    public static void pause()
    {
        EventManager.trigger("pause");
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
        EventManager.trigger("timestep:start", iteration);
        if (mh.timestep())
        {
            EventManager.trigger("timestep:end", iteration);
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
