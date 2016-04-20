/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.shafin.app;

import java.io.*;
import java.net.*;
import me.shafin.ui.ServerFrame;

/**
 *
 * @author SHAFIN
 */

public class SocketServer implements Runnable {

    public ServerThread clients[];
    public ServerSocket server = null;
    public Thread thread = null;
    public int clientCount = 0, port = 13000;
    public ServerFrame ui;
    public Database db;

    public SocketServer(ServerFrame frame) {

        clients = new ServerThread[50];
        ui = frame;
        db = new Database(ui.filePath);

        try {
            server = new ServerSocket(port);
            port = server.getLocalPort();
            ui.jTextArea1.append("Server started. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
            start();
        } catch (IOException ioe) {
            ui.jTextArea1.append("Can not bind to port : " + port + "\nRetrying");
            ui.RetryStart(0);
        }
    }

    public SocketServer(ServerFrame frame, int Port) {

        clients = new ServerThread[50];
        ui = frame;
        port = Port;
        db = new Database(ui.filePath);

        try {
            server = new ServerSocket(port);
            port = server.getLocalPort();
            ui.jTextArea1.append("Server started. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
            start();
        } catch (IOException ioe) {
            ui.jTextArea1.append(ioe+"\n");
        }
    }

    @Override
    public void run() {
        while (thread != null) {
            try {
                //ui.jTextArea1.append("\nWaiting for a client ...");
                addThread(server.accept());
            } catch (Exception ioe) {
                ui.jTextArea1.append(ioe+"\n");
                ui.RetryStart(0);
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.stop();
            thread = null;
        }
    }

    private int findClient(int ID) {
        for (int i = 0; i < clientCount; i++) {
            if (clients[i].getID() == ID) {
                return i;
            }
        }
        return -1;
    }

    public synchronized void handle(int ID, Message msg) {
        if (msg.content.equals(".bye")) {
            Announce("signout", "SERVER", msg.sender);
            remove(ID);
        } else if (msg.type.equals("login")) {
            if (findUserThread(msg.sender) == null) {
                if (db.checkLogin(msg.sender, msg.content)) {
                    clients[findClient(ID)].username = msg.sender;
                    clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                    Announce("newuser", "SERVER", msg.sender);
                    SendUserList(msg.sender);
                } else {
                    clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
                }
            } else {
                clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
            }
        } else if (msg.type.equals("message")) {
            if (msg.recipient.equals("All")) {
                Announce("message", msg.sender, msg.content);
            } else {
                findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                clients[findClient(ID)].send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
            }
        } else if (msg.type.equals("test")) {
            clients[findClient(ID)].send(new Message("test", "SERVER", "OK", msg.sender));
        } else if (msg.type.equals("signup")) {
            if (findUserThread(msg.sender) == null) {
                if (!db.userExists(msg.sender)) {
                    db.addUser(msg.sender, msg.content);
                    clients[findClient(ID)].username = msg.sender;
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender));
                    clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                    Announce("newuser", "SERVER", msg.sender);
                    SendUserList(msg.sender);
                } else {
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
                }
            } else {
                clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
            }
        } else if (msg.type.equals("upload_req")) {
            if (msg.recipient.equals("All")) {
                clients[findClient(ID)].send(new Message("message", "SERVER", "You shouldnot upload to all.", msg.sender));
            } else {
                findUserThread(msg.recipient).send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
            }
        } else if (msg.type.equals("upload_res")) {
            if (!msg.content.equals("NO")) {
                String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress();
                findUserThread(msg.recipient).send(new Message("upload_res", IP, msg.content, msg.recipient));
            } else {
                findUserThread(msg.recipient).send(new Message("upload_res", msg.sender, msg.content, msg.recipient));
            }
        }
    }

    public void Announce(String type, String sender, String content) {
        Message msg = new Message(type, sender, content, "All");
        for (int i = 0; i < clientCount; i++) {
            clients[i].send(msg);
        }
    }

    public void SendUserList(String toWhom) {
        for (int i = 0; i < clientCount; i++) {
            findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
        }
    }

    public ServerThread findUserThread(String usr) {
        for (int i = 0; i < clientCount; i++) {
            if (clients[i].username.equals(usr)) {
                return clients[i];
            }
        }
        return null;
    }

    public synchronized void remove(int ID) {
        int pos = findClient(ID);
        if (pos >= 0) {
            ServerThread toTerminate = clients[pos];
            ui.jTextArea1.append("\nRemoving client thread " + ID + " at " + pos);
            if (pos < clientCount - 1) {
                for (int i = pos + 1; i < clientCount; i++) {
                    clients[i - 1] = clients[i];
                }
            }
            clientCount--;
            try {
                toTerminate.close();
            } catch (IOException ioe) {
                ui.jTextArea1.append(ioe+"\n");
            }
            toTerminate.stop();
        }
    }

    private void addThread(Socket socket) {
        if (clientCount < clients.length) {
            ui.jTextArea1.append("\nClient connected @IP: " + socket.getInetAddress().getHostAddress()+" @Port: "+socket.getPort());
            clients[clientCount] = new ServerThread(this, socket);
            try {
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
            } catch (IOException ioe) {
                ui.jTextArea1.append(ioe+"\n");
            }
        } else {
            ui.jTextArea1.append("\nClient refused: maximum " + clients.length + " reached.");
        }
    }
}
