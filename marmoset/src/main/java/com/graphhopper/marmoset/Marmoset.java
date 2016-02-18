package com.graphhopper.marmoset;

import fi.iki.elonen.SimpleWebServer;
import fi.iki.elonen.util.ServerRunner;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;

/**
 * Created by alexander on 16/02/2016.
 */
public class Marmoset {

    private static class MarmosetSocketServer extends WebSocketServer {

        private HashSet<WebSocket> sockets;

        public MarmosetSocketServer(InetSocketAddress address)
        {
            super(address);
            sockets = new HashSet<WebSocket>();
        }

        public void distributeData(String s) {


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

    private static MarmosetHopper mh;

    public static void main(String[] args) throws IOException
    {
        mh = new MarmosetHopper();
        mh.init();

        startFileServer();
        startWebSocketServer();

    }

    private static void startWebSocketServer()
    {
        int port = 8888;

        WebSocketServer server = new MarmosetSocketServer(new InetSocketAddress(port));
        server.start();
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
