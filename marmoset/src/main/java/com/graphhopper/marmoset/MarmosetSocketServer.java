package com.graphhopper.marmoset;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashSet;

/**
 * Created by alexander on 18/02/2016.
 */
public class MarmosetSocketServer extends WebSocketServer {

    private HashSet<WebSocket> sockets;

    public MarmosetSocketServer(InetSocketAddress address)
    {
        super(address);
        sockets = new HashSet<WebSocket>();
    }

    public void distributeData(String s)
    {
        for (WebSocket socket : sockets) {
            socket.send(s);
        }
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
                                                       System.out.println("Message:" + s);
                                                                                          }

    @Override
    public void onError(WebSocket webSocket, Exception e)
    {
        System.out.println("Error :(");
        sockets.remove(webSocket);
        e.printStackTrace();
    }
}
