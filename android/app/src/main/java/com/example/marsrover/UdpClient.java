package com.example.marsrover;


import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {


    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages

    String address;

    InetAddress serverAddr;
    int port = 5000;
    DatagramSocket socket;





    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public UdpClient(String address, int port, OnMessageReceived listener) {
        mMessageListener = listener;
        this.address = address;
        this.port = port;

}

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {

                    if (socket!= null){

                        byte[] buf = (message).getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, port);
                        socket.send(packet);

                    }

                }

                catch (IOException e){

                    Log.e("UDP", e.getMessage());

                }


            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;


        mMessageListener = null;

        mServerMessage = null;
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            serverAddr = InetAddress.getByName(address);


            //create a socket to make the connection with the server
            socket = new DatagramSocket(port);

            /*byte[] buf = ("The String to Send").getBytes();

            DatagramPacket outpacket = new DatagramPacket(buf, buf.length,serverAddr, port);
            socket.send(outpacket);*/

            try {


                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    byte[] message = new byte[8000];
                    DatagramPacket inpacket = new DatagramPacket(message,message.length);
                    socket.receive(inpacket);
                    mServerMessage = new String(message, 0, inpacket.getLength());

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }



            } catch (Exception e) {
                Log.e("UDP", "S: Error", e);
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {
            Log.e("UDP", "C: Error", e);
        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}