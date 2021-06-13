package com.example.pumpbot;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class TcpClient {
    private String TAG = getClass().getName();

    static final int SERVER_PORT = 65432;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;

    private Socket mSocket;

    private ByteArrayOutputStream mTxBuffer;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(OnMessageReceived listener) {
        mTxBuffer = new ByteArrayOutputStream();
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param cmd text entered by client
     */
    public void sendMessage(String cmd) {
        Log.d(TAG, "Send: " + cmd);
        try {
            mTxBuffer.write(cmd.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        mRun = false;
    }

    public boolean connect(final String url) {
        try {
            mSocket = new Socket();
            try {
                mSocket.connect(new InetSocketAddress(url, SERVER_PORT), 5000);
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
                mSocket.close();
                return false;
            }

            mSocket.setSoTimeout(1000);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }

        return true;
    }

    public void run() {
        int nRead = 0;
        byte[] data = new byte[16384];
        mRun = true;

        try {
            try {
                while (mRun) {
                    try {
                        nRead = mSocket.getInputStream().read(data, 0, data.length);
                    } catch (SocketTimeoutException e) {
                        //all good
                        Log.d(TAG, "S: Timeout");
                    }
                    if (nRead > 0) {
                        mMessageListener.messageReceived(data, nRead);
                    }
                    if (nRead == -1) {
                        Log.i(TAG, "remote closed");
                        mRun = false;
                    }
                    if (mTxBuffer.size() > 0) {
                        try {
                            mSocket.getOutputStream().write(mTxBuffer.toByteArray());
                            mSocket.getOutputStream().flush();
                        } catch (IOException e) {
                            Log.e(TAG, e.toString());
                        }
                        mTxBuffer.reset();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "S: Error", e);
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                mSocket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "C: Error", e);
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(byte[] data, int len);
    }
}
