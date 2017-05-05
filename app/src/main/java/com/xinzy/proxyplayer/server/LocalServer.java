package com.xinzy.proxyplayer.server;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Xinzy on 2017-05-04.
 */

public class LocalServer extends Server
{
    private static final String TAG = "LocalServer";

    public LocalServer(int port)
    {
        super(port);
    }

    @Override
    protected Response serve(IHTTPSession session)
    {
        File file = new File(mUri);
        if (! file.exists())
        {
            return super.serve(session);
        }

        long startPosition = range(session);
        long contentLength = file.length();

        try
        {
            FileInputStream fis = new FileInputStream(file);
            if (startPosition > 0)
            {
                fis.skip(startPosition);
            }

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(startPosition);

            Response response = Response.newChunkedResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(mUri), fis);
            String contentRange = String.format(CONTENT_RANGE_FORMAT, startPosition, contentLength, contentLength - startPosition);
            response.addHeader("Content-Range", contentRange);

            return response;
        } catch (IOException e)
        {
            e(TAG, "serve: ", e);
        }

        return super.serve(session);
    }
}
