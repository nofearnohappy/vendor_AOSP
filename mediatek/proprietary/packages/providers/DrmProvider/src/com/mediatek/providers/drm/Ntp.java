package com.mediatek.providers.drm;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

public class Ntp {
    private static final String TAG = "Ntp";
    // if ntp encoutered exception, set offset as INVALID_OFFSET
    public static final int INVALID_OFFSET = 0x7fffffff;
    private static final double JAN_1970_1900 = ((1970 - 1900) * 365 + 17) * 24 * 60 * 60.0;
    private static long sSentTime = 0;
    public static int sync(String host) {
        int retry = 2;
        int port = 123;
        int timeout = 3000;

        // get the address and NTP address request
        //
        InetAddress ipv4Addr = null;
        try {
            Log.v(TAG, "get address from host: " + host);
            ipv4Addr = InetAddress.getByName(host);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            return INVALID_OFFSET;
        }

        int ntpServiceStatus = -1;
        DatagramSocket datagramSocket = null;
        long responseTime = -1;
        int offset = 0;
        try {
            Log.v(TAG, "create datagram socket");
            datagramSocket = new DatagramSocket();
            //Make sure this operation won't wait forever
            //If timeout then InterruptedIOException will be raised
            datagramSocket.setSoTimeout(timeout);
            int tryCount = 0;
            while (tryCount <= retry && ntpServiceStatus != 1) {
                ++tryCount;
                try {
                    // Send NTP request to NTP server
                    byte[] data = new NtpMessage().toByteArray();
                    DatagramPacket ntpRequest =
                        new DatagramPacket(data, data.length, ipv4Addr, port);
                    long sentTime = System.currentTimeMillis();
                    sSentTime = sentTime;
                    datagramSocket.send(ntpRequest);
                    Log.v(TAG, "sent via datagram socket");

                    // Read NTP Response
                    DatagramPacket ntpResponse =
                        new DatagramPacket(data, data.length);
                    datagramSocket.receive(ntpResponse);
                    responseTime = System.currentTimeMillis() - sentTime;
                    double destinationTimestamp =
                        (System.currentTimeMillis() / 1000.0) + JAN_1970_1900;

                    // Check the NTP Response
                    // If packet does not decode as expected, then IOException will be thrown
                    NtpMessage ntpMessage = new NtpMessage(ntpResponse.getData());
                    double localClockOffset =
                        ((ntpMessage.mReceiveTimestamp - ntpMessage.mOriginateTimestamp)
                         + (ntpMessage.mTransmitTimestamp - destinationTimestamp)) / 2;
                    offset = (int) localClockOffset;

                    Log.d(TAG, "local clock offset: " + offset);
                    ntpServiceStatus = 1;
                } catch (InterruptedIOException ex) {
                    Log.d(TAG, "InterruptedIOException caught, set offset as " + INVALID_OFFSET);
                    offset = INVALID_OFFSET;
                }
            }
        } catch (NoRouteToHostException e) {
            Log.e(TAG, "No route to host exception for address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } catch (ConnectException e) {
            // Connection refused. Continue to retry.
            e.fillInStackTrace();
            Log.e(TAG, "Connection exception for address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } catch (IOException ex) {
            ex.fillInStackTrace();
            Log.e(TAG, "IOException while polling address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }

        return offset;
    }

    public static long getSentTime() {
        return sSentTime;
    }
}
