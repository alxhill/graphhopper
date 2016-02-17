package com.graphhopper.marmoset;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.util.ServerRunner;

import java.io.File;
import java.io.IOException;

/**
 * Created by alexander on 16/02/2016.
 */
public class Marmoset {

    private static class MarmosetWebSocket extends NanoWSD.WebSocket {

        public MarmosetWebSocket(NanoHTTPD.IHTTPSession handshakeRequest)
        {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen()
        {
            System.out.println("OPEN!");
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode closeCode, String s, boolean b)
        {

            System.out.println("CLOSE!");
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame webSocketFrame)
        {
            System.out.println("Message: " + webSocketFrame.getTextPayload());
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame webSocketFrame)
        {
            System.out.println("PONG!");
        }

        @Override
        protected void onException(IOException e)
        {
            e.printStackTrace();
        }
    }

    private static class MarmosetSocketServer extends NanoWSD {

        public MarmosetSocketServer(String hostname, int port)
        {
            super(hostname, port);
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession ihttpSession)
        {
            System.out.println("Opening web socket");
            return new MarmosetWebSocket(ihttpSession);
        }
    }

    public static void main(String[] args) throws IOException
    {
        final File rootDir = new File("marmoset/src/main/webapp").getAbsoluteFile();
        new Thread() {
            @Override
            public void run() {
                ServerRunner.executeInstance(new SimpleWebServer(null, 8080, rootDir, true, null));
            }
        }.start();

        NanoWSD server = new MarmosetSocketServer(null, 8888);

        try
        {
            server.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        MarmosetHopper mh = new MarmosetHopper();
        mh.init();
    }
}
