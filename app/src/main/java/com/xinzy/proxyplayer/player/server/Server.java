package com.xinzy.proxyplayer.player.server;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

/**
 * Created by Xinzy on 2017-05-04.
 */

public abstract class Server extends NanoHTTPD implements IServer
{
    private static final String TAG = "Server";
    private static final String URL = "http://127.0.0.1:{port}";

    protected String mUri;
    
    public Server(int port)
    {
        super(port);
    }

    @Override
    public void startServer(String uri)
    {
        Log.v(TAG, "startServer");
        mUri = uri;
        try
        {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e)
        {
            Log.e(TAG, "startServer: error ", e);
        }
    }

    @Override
    public void stopServer()
    {
        Log.v(TAG, "stopServer");
        closeAllConnections();
        stop();
    }

    protected long range(IHTTPSession session)
    {
        long startPosition = 0;
        Map<String, String> headers = session.getHeaders();
        for (String key : headers.keySet()) {
            String header = headers.get(key);
            if ("range".equals(key) && header != null && header.startsWith("bytes=")) {
                String range = header.replace("bytes=", "").trim();
                String[] tmp = range.split("-");
                try {
                    startPosition = Long.parseLong(tmp[0]);
                } catch (NumberFormatException e) {
                }

                break;
            }
        }
        return startPosition;
    }

    public static String getAddress(int port)
    {
        return URL.replace("{port}", String.valueOf(port));
    }

    public static int getAvailablePort()
    {
        int port = 10000;
//        while (!isPortAvailable(port ++))
//        {
//        }
        return port;
    }

    private static boolean isPortAvailable(int port)
    {
        try
        {
            Socket socket = new Socket("127.0.0.1", port);
            socket.close();
            return true;
        } catch (IOException e)
        {
            return false;
        }
    }
}
