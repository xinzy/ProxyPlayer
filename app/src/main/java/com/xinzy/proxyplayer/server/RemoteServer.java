package com.xinzy.proxyplayer.server;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Xinzy on 2017-05-04.
 */

public class RemoteServer extends Server
{
    private static final String TAG = "RemoteServer";

    public static final String CONTENT_RANGE_FORMAT = "byte %d-%d/%d";

    private OkHttpClient mClient;

    public RemoteServer(int port)
    {
        super(port);
        mClient = new OkHttpClient.Builder().build();
    }

    @Override
    protected Response serve(IHTTPSession session)
    {
        Request request = new Request.Builder().url(mUri).head().build();
        try
        {
            okhttp3.Response response = mClient.newCall(request).execute();
            if (! response.isSuccessful())
            {
                return super.serve(session);
            }
            Headers headers = response.headers();
            long contentLength = 0;
            String contentType = headers.get("Content-Type");
            try
            {
                contentLength = Long.parseLong(headers.get("Content-Length"));
            } catch (NumberFormatException e)
            {
            }

            long startPosition = range(session);
            if (startPosition >= contentLength)
            {
                startPosition = 0;
            }
            Request.Builder requestBuilder = new Request.Builder().url(mUri).get();
            String range;
            if (startPosition > 0)
            {
                requestBuilder.header("Range", "bytes=" + startPosition + "-");
                range = String.format(Locale.getDefault(), CONTENT_RANGE_FORMAT, startPosition, contentLength,
                        contentLength - startPosition);
            } else
            {
                range = String.format(Locale.getDefault(), CONTENT_RANGE_FORMAT, 0, contentLength, contentLength);
            }

            response = mClient.newCall(requestBuilder.build()).execute();
            if (!response.isSuccessful())
            {
                return super.serve(session);
            }

            Response sendResponse = Response.newChunkedResponse(Status.OK, contentType, response.body().byteStream());
            sendResponse.addHeader("Content-Range", range);

            return sendResponse;
        } catch (IOException e)
        {
            Log.e(TAG, "serve: ", e);
            return super.serve(session);
        }
    }
}
