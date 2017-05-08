package com.xinzy.proxyplayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.xinzy.proxyplayer.player.InternalMediaPlayer;
import com.xinzy.proxyplayer.player.Player;
import com.xinzy.proxyplayer.player.PlayerImpl;
import com.xinzy.proxyplayer.server.LocalServer;
import com.xinzy.proxyplayer.server.RemoteServer;
import com.xinzy.proxyplayer.server.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, InternalMediaPlayer.PlayerCallback
{
    public static final String PATH = new File(Environment.getExternalStorageDirectory(), "0.mp3").getAbsolutePath();
    public static final String URL = "http://pms.ipo.com/download/attachments/62921613/download.mp3";

    private SeekBar mSeekBar;
    private TextView mCurrentTimeText;
    private TextView mTotalTimeText;

    private Player mPlayer;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mCurrentTimeText = (TextView) findViewById(R.id.currentTime);
        mTotalTimeText = (TextView) findViewById(R.id.totalTime);

        mSeekBar.setOnSeekBarChangeListener(this);
        mPlayer = new PlayerImpl(getApplicationContext());
        mPlayer.setPlayerCallback(this);

        findViewById(R.id.local).setOnClickListener(this);
        findViewById(R.id.remote).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.testLocal).setOnClickListener(this);
        findViewById(R.id.testRemoteMp3).setOnClickListener(this);
        findViewById(R.id.testLocalMp3).setOnClickListener(this);

        mHandler = new Handler();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mPlayer.stop();
        mPlayer.release();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.local:
                if (! handlerPlayer())
                {
                    mPlayer.reset();
                    mPlayer.start(Uri.parse(PATH));
                }
                break;
            case R.id.remote:
                if (! handlerPlayer())
                {
                    mPlayer.reset();
                    mPlayer.start(Uri.parse(URL));
                }
                break;
            case R.id.stop:
                mPlayer.stop();
                break;
            case R.id.testLocal:
                testServer();
                break;
            case R.id.testRemoteMp3:
                testRemoteRange();
                break;
            case R.id.testLocalMp3:
                testLocalRange();
                break;
        }
    }

    private boolean handlerPlayer()
    {
        final Player.PlayerStatus status = mPlayer.getStatus();
        if (status == Player.PlayerStatus.Playing)
        {
            mPlayer.pause();
            return true;
        } else if (status == Player.PlayerStatus.Pause)
        {
            mPlayer.resume();
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        if (fromUser)
        {
            mPlayer.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
    }

    @Override
    public void onBufferingUpdate(InternalMediaPlayer player, int percent)
    {
        int max = mSeekBar.getMax();
        if (max > 0)
        {
            mSeekBar.setSecondaryProgress(max * percent / 100);
        }
    }

    @Override
    public void onPrepared(InternalMediaPlayer player)
    {
    }

    @Override
    public void onCompletion(InternalMediaPlayer player)
    {
    }

    @Override
    public void onProgressUpdate(InternalMediaPlayer player, int current, int total)
    {
        mSeekBar.setMax(total);
        mSeekBar.setProgress(current);
        mCurrentTimeText.setText(getTime(current));
        mTotalTimeText.setText(getTime(total));
    }

    private String getTime(int time)
    {
        int sec = time / 1000;

        int second = sec % 60;
        sec /= 60;
        int min = sec % 60;
        int hour = sec / 60;

        StringBuffer sb = new StringBuffer();
        if (hour > 0)
        {
            sb.append(hour).append(":");
        }
        if (min < 10)
        {
            sb.append(0);
        }
        sb.append(min).append(":");
        if (second < 10)
        {
            sb.append(0);
        }
        sb.append(second);

        return sb.toString();
    }









    /////////////////////////////////////////////////////////////////////
    // 以下是测试代码
    /////////////////////////////////////////////////////////////////////


    private void testServer()
    {
        int port = Server.getAvailablePort();
        LocalServer server = new LocalServer(port);
        server.startServer(PATH);

        new TestThread(port).start();
    }

    class TestThread extends Thread
    {
        int port;

        public TestThread(int port)
        {
            super();
            this.port = port;
        }

        @Override
        public void run()
        {
            try
            {
                URL url = new URL(Server.getAddress(port));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream is = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "download.mp3"));

                int SIZE = 10240;
                byte[] buffer = new byte[SIZE];
                int size;
                while ((size = is.read(buffer, 0, SIZE)) > 0)
                {
                    fos.write(buffer, 0, size);
                }
                fos.close();
                is.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void testLocalRange()
    {
        int port = Server.getAvailablePort();
        Server server = new RemoteServer(port);
        server.startServer(URL);

        mHandler.postDelayed(() -> {new RangeThread(RangeThread.HEADER, Server.getAddress(port)).start();}, 1000);

    }

    private void testRemoteRange()
    {
        new RangeThread(RangeThread.HEADER | RangeThread.TAIL, URL).start();
    }

    class RangeThread extends Thread
    {
        private static final String TAG = "RangeThread";
        private static final int BUFFER_SIZE = 8096;

        private static final int HEADER = 0x1;
        private static final int TAIL = 0x2;

        private int mode;
        private String url;

        RangeThread(int mode, String url)
        {
            this.url = url;
            this.mode = mode;

            Log.i(TAG, "RangeThread: url = " + url);
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
                raf.close();
            } catch (IOException e)
            {
            }

            Log.i(TAG, "download end ");
        }

        void logHeader(Headers headers)
        {
            StringBuffer sb = new StringBuffer("Test header: ");
            for (String s : headers.names())
            {
                sb.append(s).append(':').append(headers.get(s)).append("\n");
            }
            Log.v(TAG, sb.toString());
        }
    }

}
