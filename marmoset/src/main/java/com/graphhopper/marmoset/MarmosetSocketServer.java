package com.graphhopper.marmoset;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

    public synchronized void distributeData(String s)
    {
        sockets.stream().forEach(socket -> socket.send(s));
    }

    public void distributeData(ByteBuffer data)
    {
        sockets.stream().forEach(socket -> socket.send(data));
    }

    @Override
    public void stop()
    {
        try
        {
            super.stop();
        }
        catch (IOException | InterruptedException ignored)
        {}
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
    {
        logger.debug("Opened WebSocket:" + webSocket.getLocalSocketAddress() + "->" + webSocket.getRemoteSocketAddress());
        sockets.add(webSocket);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b)
    {
        logger.debug("Closed WebSocket:" + webSocket.getLocalSocketAddress() + "->" + webSocket.getRemoteSocketAddress());
        sockets.remove(webSocket);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s)
    {
        logger.debug("Received message: " + s);
        if (s.matches("start\\|\\d+"))
        {
            String num = s.split("\\|")[1];
            Marmoset.start(Integer.valueOf(num, 10));
        }
        else if (s.matches("addVehicles\\|\\d+"))
        {
            int num = Integer.valueOf(s.split("\\|")[1], 10);
            Marmoset.addVehicles(num);
        }
        else if (s.equals("pause"))
        {
            Marmoset.pause();
        }
        else if (s.equals("next"))
        {
            Marmoset.nextTimestep();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e)
    {
        e.printStackTrace();
        if (webSocket != null)
            sockets.remove(webSocket);
    }
}
