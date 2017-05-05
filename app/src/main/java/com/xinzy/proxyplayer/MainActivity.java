package com.xinzy.proxyplayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.xinzy.proxyplayer.player.InternalMediaPlayer;
import com.xinzy.proxyplayer.player.Player;
import com.xinzy.proxyplayer.player.PlayerImpl;
import com.xinzy.proxyplayer.server.LocalServer;
import com.xinzy.proxyplayer.server.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, InternalMediaPlayer.PlayerCallback
{
    public static final String PATH = new File(Environment.getExternalStorageDirectory(), "0.mp3").getAbsolutePath();
    public static final String URL = "http://up.haoduoge.com:82/mp3/2017-05-05/1493949884.mp3";

    private SeekBar mSeekBar;
    private TextView mCurrentTimeText;
    private TextView mTotalTimeText;
    private Button mLocalBtn;
    private Button mRemoteBtn;

    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mCurrentTimeText = (TextView) findViewById(R.id.currentTime);
        mTotalTimeText = (TextView) findViewById(R.id.totalTime);
        mLocalBtn = (Button) findViewById(R.id.local);
        mRemoteBtn = (Button) findViewById(R.id.remote);

        mSeekBar.setOnSeekBarChangeListener(this);
        mLocalBtn.setOnClickListener(this);
        mRemoteBtn.setOnClickListener(this);
        mPlayer = new PlayerImpl();
        mPlayer.setPlayerCallback(this);

        findViewById(R.id.testLocal).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.local:
                mPlayer.reset();
                if (mPlayer.getStatus() == Player.PlayerStatus.Playing)
                {
                    mPlayer.stop();
                } else
                {
                    mPlayer.start(Uri.parse(PATH));
                }
                break;
            case R.id.remote:
                mPlayer.reset();
                if (mPlayer.getStatus() == Player.PlayerStatus.Playing)
                {
                    mPlayer.stop();
                } else
                {
                    mPlayer.start(Uri.parse(URL));
                }
                break;

            case R.id.testLocal:
                testServer();
                break;
        }
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

}
