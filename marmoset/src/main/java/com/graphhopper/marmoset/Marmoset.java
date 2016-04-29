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
    private static boolean serverEnabled = true;

    private static Logger logger = LoggerFactory.getLogger(Marmoset.class);

    private static int iteration;

    public static String metricFolder;

    public static int ITERATION_OUTPUT_FREQ = 1000;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        EventManager.trigger("init:start");
        mh = new MarmosetHopper();
        mh.init();

        if (args.length == 0 || args[0].equals("--web"))
        {
            initialiseMetrics("realtime");
            startFileServer();
            startWebSocketServer();
        } else if (args[0].equals("--file")) {
            serverEnabled = false;
            String name = args[1];
            if (args.length > 2)
                name += "-" + args[2];
            initialiseMetrics(name);
            runOfflineSimulation(Integer.parseInt(args[1]));
            logger.info("Simulation complete.");
            return;
        }

        System.out.println("Press enter to terminate");
        EventManager.trigger("init:end");
        try
        {
            System.in.read();
        } catch (Throwable ignored) {}

        EventManager.trigger("online:stop");

        System.exit(0);
    }

    private static void initialiseMetrics(String name) throws FileNotFoundException, UnsupportedEncodingException
    {
        metricFolder = "simulations/" + name + "-" + (System.currentTimeMillis() / 1000L);
        new File(metricFolder + "/vehicles").mkdirs();

        PrintWriter p = new PrintWriter(metricFolder + "/simulation.csv", "UTF-8");
        p.println(MarmosetHopper.Metrics.getHeader());

        EventManager.listenTo("timestep:end", (n, a) -> {
            int iteration = (Integer) a[0];
            if (iteration % ITERATION_OUTPUT_FREQ == 0)
            {
                mh.updateLocations();
                try
                {
                    PrintWriter iterPrint = new PrintWriter(metricFolder + "/iteration" + iteration, "UTF-8");
                    iterPrint.println(mh.getVehicleString());
                    iterPrint.close();
                } catch (IOException ignored) {}
            }
        });

        EventManager.listenTo("timestep:metrics", (n, m) -> {
            p.println(m[0].toString());
            p.flush();
        });

        EventManager.listenTo("offline:stop", (n, a) -> p.close());
        EventManager.listenTo("online:stop", (n, a) -> p.close());
    }

    private static void runOfflineSimulation(int initialVehicles) throws IOException
    {
        start(initialVehicles);
        int vehCount = mh.getVehicleCount();
        while (mh.getVehicleCount() > 0)
        {
            nextTimestep();
            // termination conditions so it doesn't loop endlessly if one or two get stuck
            if (vehCount == mh.getVehicleCount() &&
                    mh.getMetrics().averageCells == 0 &&
                    vehCount < 50)
            {
                logger.info("Terminating early due to permanent loop occurring");
                break;
            }
        }
        EventManager.trigger("offline:stop");
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
            logger.info("===ITERATION [" + iteration + "] VEHICLES [" + mh.getVehicleCount() + "]===");
            MarmosetHopper.Metrics metrics = mh.getMetrics();
            if (metrics == null)
                return;
            logger.info(metrics.getDescription());

            EventManager.trigger("timestep:metrics", metrics);

            iteration++;

            if (serverEnabled)
            {
                ByteBuffer data = mh.getVehicleBytes();
                mss.distributeData(data);
            }

        }
        else
        {
            logger.info("Timestep failed, not iterating");
        }
    }
}
