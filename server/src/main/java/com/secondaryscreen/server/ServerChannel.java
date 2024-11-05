package com.secondaryscreen.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public abstract class ServerChannel {
    private Thread mThread;
    private int mPort;
    public ServerChannel(int port) {
        mPort = port;
        mThread = new ServerChannelThread();
    }

    public void start() {
        mThread.start();
    }

    public void join() throws InterruptedException {
        if (mThread != null) {
            mThread.join();
        }
    }

    public abstract void work(byte[] buffer, int length);
    public void accept(String remoteAddress) { }

    private class ServerChannelThread extends Thread {
        public ServerChannelThread() {
            super("ServerChannelThread");
            System.out.println("ServerChannelThread");
        }
        @Override
        public void run() {
            try (Selector selector = Selector.open();
                 ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
                serverSocket.socket().bind(new InetSocketAddress(mPort));
                serverSocket.socket().setReuseAddress(true);
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                ByteBuffer headerBuffer = ByteBuffer.allocate(4);
                ByteBuffer eventBuffer = ByteBuffer.allocate(0);

                SocketChannel currentSocketChannel = null;

                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select() != 0) {
                        Set keys = selector.selectedKeys();
                        Iterator iterator = keys.iterator();

                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            iterator.remove();

                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                SocketChannel socketChannel = server.accept();
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);

                                if (currentSocketChannel != null) {
                                    currentSocketChannel.close();
                                    System.out.println("close old sockectChannel");
                                }
                                currentSocketChannel = socketChannel;

                                SocketAddress socketAddress = socketChannel.socket().getRemoteSocketAddress();
                                if (socketAddress instanceof InetSocketAddress) {
                                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                                    // getHostName有时会查询NDS，8～10秒才会退出，所以改用getHostAddress
                                    // accept(inetSocketAddress.getHostName());
                                    accept(inetSocketAddress.getAddress().getHostAddress());
                                } else {
                                    System.out.println("socketAddress is not InetSocketAddress");
                                }
                            }
                            if (key.isReadable()) {
                                SocketChannel socketChannel = (SocketChannel) key.channel();

                                try {
                                    recv(socketChannel, headerBuffer, 4);
                                    int len = headerBuffer.getInt();
                                    if (eventBuffer.capacity() < len) {
                                        System.out.println("need bigger len:" + len);
                                        eventBuffer = ByteBuffer.allocate(len);
                                    }
                                    recv(socketChannel, eventBuffer, len);
                                    work(eventBuffer.array(), len);
                                } catch (Exception e) {
                                    socketChannel.close();

                                    System.out.println("ServerChannelThread recv exception:" + e);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ServerChannelThread exception:" + e);
            }
        }

        private void recv(SocketChannel socketChannel, ByteBuffer buffer, int length) throws Exception {
            buffer.clear();
            buffer.limit(length);
            while (buffer.position() != length) {
                int len = socketChannel.read(buffer);
                if (len == -1) {
                    throw new RuntimeException("socket closed");
                }
            }
            buffer.flip();
        }
    }
}