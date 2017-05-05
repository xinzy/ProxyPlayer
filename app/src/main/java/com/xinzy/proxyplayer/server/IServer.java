package com.xinzy.proxyplayer.server;

/**
 * Created by Xinzy on 2017-05-05.
 */

public interface IServer
{
    boolean DEBUG = true;

    void stopServer();

    void startServer(String uri);
}
