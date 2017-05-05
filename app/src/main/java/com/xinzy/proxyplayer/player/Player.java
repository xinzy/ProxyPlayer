package com.xinzy.proxyplayer.player;

import android.net.Uri;

/**
 * Created by Xinzy on 2017-05-04.
 */

public interface Player
{

    void start(Uri uri);

    void stop();

    void pause();

    void seekTo(int msec);

    void release();

    void reset();

    PlayerStatus getStatus();

    void setPlayerCallback(InternalMediaPlayer.PlayerCallback callback);

    /**
     * 播放器状态
     */
    enum PlayerStatus {
        /**
         * 播放中
         */
        Playing,

        /**
         * 暂停中
         */
        Pause,

        /**
         * 停止
         */
        Stop,

        /**
         * 准备播放
         */
        Preparing,
    }
}
