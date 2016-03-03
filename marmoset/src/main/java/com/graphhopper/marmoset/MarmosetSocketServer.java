package com.graphhopper.marmoset;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by alexander on 18/02/2016.
 */
public class MarmosetSocketServer extends WebSocketServer {

    private Set<WebSocket> sockets;
    private Logger logger = LoggerFactory.getLogger(MarmosetSocketServer.class);

    public MarmosetSocketServer(InetSocketAddress address)
    {
        super(address);
        sockets = Collections.synchronizedSet(new HashSet<>());
    }

    public void distributeData(String s)
    {
        sockets.stream().forEach(socket -> socket.send(s));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
    {
        System.out.println("OPEN!" + webSocket.toString());
        sockets.add(webSocket);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b)
    {
        System.out.println("CLOSE!" + webSocket.toString());
        sockets.remove(webSocket);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s)
    {
        logger.info("Received message: " + s);
        if (s.matches("start\\|\\d+"))
        {
            logger.info("run message");
            String num = s.split("\\|")[1];
            logger.info("Starting simulation with " + num + " vehicles");
            Marmoset.run(Integer.valueOf(num, 10));
        }
        else if (s.equals("addVehicle"))
        {
            Marmoset.addVehicle();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e)
    {
        System.out.println("Error :(");
        sockets.remove(webSocket);
        e.printStackTrace();
    }
}
