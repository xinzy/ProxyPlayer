package com.xinzy.proxyplayer.player;

import android.media.MediaPlayer;
import android.os.Handler;

/**
 * Created by Xinzy on 2017-05-04.
 */

public class InternalMediaPlayer extends MediaPlayer implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener
{
    private static final int PROGRESS_UPDATE_PERIOD = 1000;

    private Player.PlayerStatus mStatus;

    private PlayerCallback mCallback;

    private Handler mHandler;

    private Runnable mProgressUpdateTask = new Runnable()
    {
        @Override
        public void run()
        {
            int current = getCurrentPosition();
            int total = getDuration();

            if (mCallback != null)
            {
                mCallback.onProgressUpdate(InternalMediaPlayer.this, current, total);
            }
            mHandler.postDelayed(mProgressUpdateTask, PROGRESS_UPDATE_PERIOD);
        }
    };

    public InternalMediaPlayer()
    {
        setOnBufferingUpdateListener(this);
        setOnPreparedListener(this);
        setOnCompletionListener(this);

        mHandler = new Handler();
        mStatus = Player.PlayerStatus.Stop;
    }

    public Player.PlayerStatus getStatus()
    {
        return mStatus;
    }

    public void setCallback(PlayerCallback callback)
    {
        this.mCallback = callback;
    }

    @Override
    public void start() throws IllegalStateException
    {
        super.start();
        mStatus = Player.PlayerStatus.Playing;
        mHandler.post(mProgressUpdateTask);
    }

    @Override
    public void pause() throws IllegalStateException
    {
        super.pause();
        mStatus = Player.PlayerStatus.Pause;
        mHandler.removeCallbacks(mProgressUpdateTask);
    }

    @Override
    public void stop() throws IllegalStateException
    {
        super.stop();
        mStatus = Player.PlayerStatus.Stop;
        mHandler.removeCallbacks(mProgressUpdateTask);
    }

    @Override
    public void prepareAsync() throws IllegalStateException
    {
        super.prepareAsync();
        mStatus = Player.PlayerStatus.Preparing;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
        if (mCallback != null)
        {
            mCallback.onBufferingUpdate(this, percent);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        if (mStatus == Player.PlayerStatus.Preparing)
        {
            start();
        }

        if (mCallback != null)
        {
            mCallback.onPrepared(this);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        mStatus = Player.PlayerStatus.Stop;
        mHandler.removeCallbacks(mProgressUpdateTask);

        if (mCallback != null)
        {
            mCallback.onCompletion(this);
        }
    }

    public interface PlayerCallback
    {
        void onBufferingUpdate(InternalMediaPlayer player, int percent);

        void onPrepared(InternalMediaPlayer player);

        void onCompletion(InternalMediaPlayer player);

        void onProgressUpdate(InternalMediaPlayer player, int current, int total);
    }
}
