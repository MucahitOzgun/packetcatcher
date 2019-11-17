package com.company.sql;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import org.apache.log4j.Logger;

import com.company.sql.util.ByteUtil;
import com.company.sql.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Scanner;

public class SqlProxyHandler extends ChannelInboundHandlerAdapter{

	private static final Logger logger = Logger.getLogger (SqlProxyHandler.class);

	private final int remotePort;
	private final String remoteHost;
	private Channel inboundChannel;
	private Channel outboundChannel;
	private SqlProxyBackendHandler  backendHandler;
	private boolean waitingStatus=false;
	private final LinkedList<Object> inboundMsgBuffer = new LinkedList<Object> ();

	enum ConnectionStatus{
		init,
		outBoundChnnlConnecting,      //inbound connected and outbound connecting
		outBoundChnnlReady,           //inbound connected and outbound connected
		closing                       //closing inbound and outbound connection
	}

	private ConnectionStatus connectStatus = ConnectionStatus.init;



	SqlProxyHandler(String remoteHost, int remotePort){
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("channelActive");
		inboundChannel = ctx.channel ();




		Bootstrap b = new Bootstrap ();
		b.group (inboundChannel.eventLoop ())
		.channel (inboundChannel.getClass ())
		.handler (new ChannelInitializer<SocketChannel> () {
			protected void initChannel(SocketChannel socketChannel) throws Exception {
				backendHandler = new SqlProxyBackendHandler (SqlProxyHandler.this);
				socketChannel.pipeline ().addLast (new StringEncoder ());
				socketChannel.pipeline ().addLast (backendHandler);
			}
		})

		.option (ChannelOption.AUTO_READ,false);

		ChannelFuture cf = b.connect (remoteHost,remotePort);
		connectStatus = ConnectionStatus.outBoundChnnlConnecting;

		outboundChannel = cf.channel ();

		cf.addListener (new ChannelFutureListener () {
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if ( channelFuture.isSuccess () ) {
					inboundChannel.read ();
				} else {
					inboundChannel.close ();
				}

			}
		});


	}


	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {

		ByteBuf copiedMsg = ((ByteBuf)msg).copy ();

		logger.debug ("Client msg: " + SqlProxyHandler.readMessage (copiedMsg));

		byte[] bytes = ByteUtil.decodeAsBytes((ByteBuf)msg);
		logger.debug("Connection packet from client;\r\n" +  ByteUtil.byteArrayToHexAndAsciiAndDecDump(
				ByteUtil.decodeAsBytes(copiedMsg)));
//		ExcelUtil.writeToFile(bytes);
		FileUtil.writeToExcel(bytes);

		

		 Scanner in = new Scanner(System.in);
		 in.nextLine();

		
		
//		ExcelUtil.byte[] newDatabin = readFromFile();
     	byte[] newDatabin = FileUtil.readFromExcel();
		ByteBuf sendPacket = ByteUtil.getByteBuf(newDatabin);
		logger.debug("Changed Packet;\r\n" +  ByteUtil.byteArrayToHexAndAsciiAndDecDump(newDatabin));

		switch(connectStatus){
		case outBoundChnnlReady:
			logger.debug("outBoundChnnlReady");
			outboundChannel.writeAndFlush (sendPacket).addListener (new ChannelFutureListener () {
				public void operationComplete(ChannelFuture channelFuture) throws Exception {
					if (channelFuture.isSuccess ()) {
						ctx.channel ().read ();
					} else {
						channelFuture.channel ().close ();
					}
				}
			});
			break;
		case closing:
			logger.debug("closing");
			release(msg);
			break;
		case init:
			logger.error("Bad connectStatus.");
			close();
			break;
		case outBoundChnnlConnecting:
			logger.debug("outBoundChnnlConnecting");
		default:
			logger.debug("default");
			inboundMsgBuffer.add(msg);
		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("channelInactive");
		if (outboundChannel != null) {
			closeOnFlush (outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace ();
		closeOnFlush (ctx.channel ());
	}

	static void closeOnFlush(Channel ch) {
		if (ch.isActive ()) {
			ch.writeAndFlush (Unpooled.EMPTY_BUFFER).addListener (ChannelFutureListener.CLOSE);
		}
	}

	void outBoundChannelReady() {
		logger.debug("outBoundChannelReady");
		inboundChannel.config().setAutoRead(true);

		connectStatus = ConnectionStatus.outBoundChnnlReady;
		for(Object obj : inboundMsgBuffer){
			outboundChannel.writeAndFlush(obj);
		}
		inboundMsgBuffer.clear();
	}

	Channel getInboundChannel() {
		logger.debug("getInboundChannel");
		return inboundChannel;
	}

	private void release(Object obj){
		if(obj instanceof ByteBuf){
			((ByteBuf)obj).release();
		}
	}
	private void close() {
		connectStatus = ConnectionStatus.closing;
		for(Object obj : inboundMsgBuffer){
			release(obj);
		}
		inboundMsgBuffer.clear();
		closeOnFlush(inboundChannel);
		closeOnFlush(outboundChannel);
	}


	static String readMessage(ByteBuf byteBuf){
		return byteBuf.toString (Charset.defaultCharset ());
	}
}
