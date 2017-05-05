package com.xinzy.proxyplayer.player;

import android.net.Uri;
import android.util.Log;

import com.xinzy.proxyplayer.server.LocalServer;
import com.xinzy.proxyplayer.server.RemoteServer;
import com.xinzy.proxyplayer.server.Server;

import java.io.IOException;

/**
 * Created by Xinzy on 2017-05-04.
 */

public class PlayerImpl implements Player, InternalMediaPlayer.PlayerCallback
{
    private static final String TAG = "PlayerImpl";
    
    private InternalMediaPlayer mMediaPlayer;
    private Server mServer;

    private InternalMediaPlayer.PlayerCallback mCallback;

    public PlayerImpl()
    {
        mMediaPlayer = new InternalMediaPlayer();
        mMediaPlayer.setCallback(this);
    }

    @Override
    public void start(Uri uri)
    {
        String scheme = uri.getScheme();
        String path = uri.getPath();
        Log.d(TAG, "player start: scheme = " + scheme + "; path = " + path + "; uri = " + uri);

        int port = Server.getAvailablePort();
        if (scheme == null || "file".equals(scheme))
        {
            mServer = new LocalServer(port);
            mServer.startServer(path);
        } else
        {
            mServer = new RemoteServer(port);
            mServer.startServer(uri.toString());
        }

        try
        {
            mMediaPlayer.setDataSource(Server.getAddress(port));
            mMediaPlayer.prepareAsync();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mServer != null)
        {
            mServer.stopServer();
        }
    }

    @Override
    public void pause()
    {
        if (mMediaPlayer.getStatus() == PlayerStatus.Playing)
        {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void seekTo(int msec)
    {
        if (mMediaPlayer.getStatus() == PlayerStatus.Playing)
        {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(msec);
            mMediaPlayer.start();
        }
    }

    @Override
    public void reset()
    {
        mMediaPlayer.reset();
    }

    @Override
    public void release()
    {
        mMediaPlayer.release();
    }

    @Override
    public PlayerStatus getStatus()
    {
        return mMediaPlayer.getStatus();
    }

    @Override
    public void setPlayerCallback(InternalMediaPlayer.PlayerCallback callback)
    {
        mCallback = callback;
    }


    @Override
    public void onBufferingUpdate(InternalMediaPlayer player, int percent)
    {
        if (mCallback != null)
        {
            mCallback.onBufferingUpdate(player, percent);
        }
    }

    @Override
    public void onPrepared(InternalMediaPlayer player)
    {
        if (mCallback != null)
        {
            mCallback.onPrepared(player);
        }
    }

    @Override
    public void onCompletion(InternalMediaPlayer player)
    {
        mServer.stopServer();
        if (mCallback != null)
        {
            mCallback.onCompletion(player);
        }
    }

    @Override
    public void onProgressUpdate(InternalMediaPlayer player, int current, int total)
    {
        if (mCallback != null)
        {
            mCallback.onProgressUpdate(player, current, total);
        }
    }
}
