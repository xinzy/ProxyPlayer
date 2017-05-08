package com.xinzy.proxyplayer.test.thread;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Xinzy on 2017-05-08.
 */

public class RangeThread extends Thread
{
    private static final String TAG = "RangeThread";
    private static final int BUFFER_SIZE = 8096;

    public static final int HEADER = 0x1;
    public static final int TAIL = 0x2;

    private int mode;
    private String url;

    public RangeThread(int mode, String url)
    {
        this.url = url;
        this.mode = mode;
    }

    @Override
    public void run()
    {
        try
        {
            long startPosition = 1690120;
            long contentLength = 4285955;
            String range = String.format(Locale.getDefault(), "byte %1d-%2d/%3d", startPosition, contentLength,
                    contentLength - startPosition);

            RandomAccessFile raf = new RandomAccessFile(new File(Environment.getExternalStorageDirectory(), "download.mp3"), "rw");
            OkHttpClient client = new OkHttpClient.Builder().build();

            Request.Builder requestBuilder;

            Response response;
            InputStream is;
            byte[] buff = new byte[BUFFER_SIZE];
            int length = 0;

            if ((mode & TAIL) > 0)
            {
                requestBuilder = new Request.Builder().url(url).get();
                requestBuilder.header("Range", "bytes=" + startPosition + "-");
                response = client.newCall(requestBuilder.build()).execute();
                logHeader(response.headers());
                raf.seek(startPosition);

                is = response.body().byteStream();
                while ((length = is.read(buff, 0, BUFFER_SIZE)) > 0)
                {
                    raf.write(buff, 0, length);
                }
                is.close();
            }

            if ((mode & HEADER) > 0)
            {
                requestBuilder = new Request.Builder().url(url).get().addHeader("Range", "bytes=0-" + (startPosition - 1));
                response = client.newCall(requestBuilder.build()).execute();
                logHeader(response.headers());
                raf.seek(0);

                is = response.body().byteStream();
                while ((length = is.read(buff, 0, BUFFER_SIZE)) > 0)
                {
                    raf.write(buff, 0, length);
                }
                is.close();

            }

            raf.close();
        } catch (IOException e)
        {
        }

        Log.i(TAG, "download end ");
    }

    void logHeader(Headers headers)
    {
        StringBuffer sb = new StringBuffer("Remote header: ");
        for (String s : headers.names())
        {
            sb.append(s).append(':').append(headers.get(s)).append("\n");
        }
        Log.v(TAG, sb.toString());
    }
}