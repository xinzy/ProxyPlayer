package com.xinzy.proxyplayer.server;

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

    private OkHttpClient mClient;

    public RemoteServer(int port)
    {
        super(port);
        mClient = new OkHttpClient.Builder().build();
    }

    @Override
    protected Response serve(IHTTPSession session)
    {
        logHeader(session);
        try
        {
            // Header 请求远程服务器。获取音频头信息
            Request request = new Request.Builder().url(mUri).head().build();
            okhttp3.Response response = mClient.newCall(request).execute();
            if (! response.isSuccessful())
            {
                return super.serve(session);
            }
            Headers headers = response.headers();
            logHeader(headers);

            long originContentLength = 0;
            String contentType = headers.get("Content-Type");
            String eTag = headers.get("ETag");
            try
            {
                originContentLength = Long.parseLong(headers.get("Content-Length"));
            } catch (NumberFormatException e)
            {
            }


            // 获取MediaPlayer真实请求的range
            long startPosition = range(session, originContentLength);


            // Get请求远程音频文件，获取输入流
            Request.Builder requestBuilder = new Request.Builder().url(mUri).get();
            long contentLength = originContentLength - startPosition;
            String range;
            if (startPosition > 0)
            {
                requestBuilder.header("Range", "bytes=" + startPosition + "-");
                range = String.format(Locale.getDefault(), CONTENT_RANGE_FORMAT, startPosition, originContentLength - 1,
                        contentLength);
            } else
            {
                range = String.format(Locale.getDefault(), CONTENT_RANGE_FORMAT, 0, originContentLength - 1, originContentLength);
            }

            i(TAG, "contentLength = " + originContentLength + "; startPosition = " + startPosition + "; range = " + range);
            response = mClient.newCall(requestBuilder.build()).execute();
            logHeader(response.headers());
            if (!response.isSuccessful())
            {
                return super.serve(session);
            }

            // 发送数据 接收方MediaPlayer
            Response sendResponse = Response.newChunkedResponse(Status.OK, contentType, response.body().byteStream());
            sendResponse.addHeader("Accept-Ranges", "bytes");
            sendResponse.addHeader("Content-Range", range);
            sendResponse.addHeader("Content-Length", String.valueOf(contentLength));
            sendResponse.addHeader("ETag", eTag);
            return sendResponse;
        } catch (IOException e)
        {
            e(TAG, "serve: ", e);
            return super.serve(session);
        }
    }
}
