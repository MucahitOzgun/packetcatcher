package com.company.sql;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.company.sql.util.FileUtil;

public class SqlProxyServer {
    private final int localPort;
    private final int remotePort;
    private final String remoteHost;

    private SqlProxyServer(int localPort, String remoteHost, int remotePort){
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }
    private static final Logger logger = Logger.getLogger (SqlProxyServer.class);
    private void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup ();
        EventLoopGroup workerGroup = new NioEventLoopGroup ();

        try {
        	
   
        	 
            logger.debug ("You listening on port:"+localPort);
            logger.debug ("Press a key for send to server the packet");
            ServerBootstrap sb = new ServerBootstrap ();
            sb.group(bossGroup, workerGroup);
            sb.channel(NioServerSocketChannel.class);
            sb.childHandler(new ChannelInitializer<SocketChannel> () {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline ().addLast (new StringEncoder ());;
                    SqlProxyHandler sqlProxyHandler = new SqlProxyHandler (remoteHost, remotePort);
                    socketChannel.pipeline().addLast(sqlProxyHandler);
                }
            });
            sb.option(ChannelOption.SO_BACKLOG, 128);
            sb.childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = sb.bind(localPort).sync();

            f.channel ().closeFuture ().sync ();


        } finally {

            bossGroup.shutdownGracefully ();
            workerGroup.shutdownGracefully ();
        }


    }

    public static void main(String[] args)  {
        try {
			new SqlProxyServer (123,"192.168.93.128",39013).run ();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
}
