/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.shafin.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import me.shafin.ui.ServerFrame;

/**
 *
 * @author SHAFIN
 */
public class ServerThread extends Thread {

    public SocketServer server = null;
    public Socket socket = null;
    public int ID = -1;
    public String username = "";
    public ObjectInputStream streamIn = null;
    public ObjectOutputStream streamOut = null;
    public ServerFrame ui;

    public ServerThread(SocketServer socketServer, Socket socket) {
        super();
        this.server = socketServer;
        this.socket = socket;
        this.ID = socket.getPort();
        this.ui = socketServer.ui;
    }

    public void send(Message msg) {
        try {
            streamOut.writeObject(msg);
            streamOut.flush();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public int getID() {
        return ID;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message msg = (Message) streamIn.readObject();
                server.handle(ID, msg);
            } catch (IOException | ClassNotFoundException ioe) {
                System.out.println(ioe);
                server.remove(ID);
                stop();
            }
        }
    }

    public void open() throws IOException {
        streamOut = new ObjectOutputStream(socket.getOutputStream());
        streamOut.flush();
        streamIn = new ObjectInputStream(socket.getInputStream());
    }

    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
        if (streamIn != null) {
            streamIn.close();
        }
        if (streamOut != null) {
            streamOut.close();
        }
    }
}
