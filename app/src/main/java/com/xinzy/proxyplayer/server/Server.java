package com.xinzy.proxyplayer.server;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import okhttp3.Headers;

/**
 * Created by Xinzy on 2017-05-04.
 */

public abstract class Server extends NanoHTTPD implements IServer
{
    private static final String TAG = "Server";
    private static final String URL = "http://127.0.0.1:{port}";

    protected static final String CONTENT_RANGE_FORMAT = "byte %1d-%2d/%3d";

    protected String mUri;
    
    public Server(int port)
    {
        super(port);
    }

    @Override
    public void startServer(String uri)
    {
        mUri = uri;
        v(TAG, "startServer: uri = " + uri + "; server port = " + getPort());
        try
        {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e)
        {
            e(TAG, "startServer: error ", e);
        }
    }

    @Override
    public void stopServer()
    {
        v(TAG, "stopServer");
        closeAllConnections();
        stop();
    }

    protected long range(IHTTPSession session, long max)
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
        return startPosition >= max ? 0 : startPosition;
    }

    void logHeader(Headers headers)
    {
        StringBuffer sb = new StringBuffer("Remote header: ");
        for (String s : headers.names())
        {
            sb.append(s).append(':').append(headers.get(s)).append("\n");
        }
        v(TAG, sb.toString());
    }

    void logHeader(IHTTPSession session)
    {
        Map<String, String> headers = session.getHeaders();
        StringBuffer sb = new StringBuffer("Server header: ");
        for (String s : headers.keySet())
        {
            sb.append(s).append(':').append(headers.get(s)).append("\n");
        }
        e(TAG, sb.toString());
    }





    ////////////////////////////////////////////////
    // Debug
    ////////////////////////////////////////////////

    protected void v(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.v(tag, msg);
        }
    }

    protected void i(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.i(tag, msg);
        }
    }

    protected void d(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.d(tag, msg);
        }
    }

    protected void e(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.e(tag, msg);
        }
    }

    protected void e(String tag, String msg, Throwable t)
    {
        if (DEBUG)
        {
            Log.e(tag, msg, t);
        }
    }





    public static String getAddress(int port)
    {
        return URL.replace("{port}", String.valueOf(port));
    }

    private static int port = 10000;
    public static int getAvailablePort()
    {
        return port ++;
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
