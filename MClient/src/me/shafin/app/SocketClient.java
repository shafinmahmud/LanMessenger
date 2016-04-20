/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.shafin.app;

import java.awt.HeadlessException;
import me.shafin.ui.ChatFrame;
import java.io.*;
import java.net.*;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class SocketClient implements Runnable {

    public int port;
    public String serverAddr;
    public Socket socket;
    public ChatFrame ui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;

    public SocketClient(ChatFrame frame) throws IOException {
        ui = frame;
        this.serverAddr = ui.serverAddr;
        this.port = ui.port;
        socket = new Socket(InetAddress.getByName(serverAddr), port);

        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                Message msg = (Message) In.readObject();
                //System.out.println("Incoming : " + msg.toString());

                switch (msg.type) {
                    case "message":
                        if (msg.recipient.equals(ui.username)) {
                            ui.jTextArea1.append("[" + msg.sender + " > Me] : " + msg.content + "\n");
                        } else {
                            ui.jTextArea1.append("[" + msg.sender + " > " + msg.recipient + "] : " + msg.content + "\n");
                        }   if (!msg.content.equals(".bye") && !msg.sender.equals(ui.username)) {
                            String msgTime = (new Date()).toString();
                            
                            try {
//                            hist.addMessage(msg, msgTime);
//                            DefaultTableModel table = (DefaultTableModel) ui.historyFrame.jTable1.getModel();
//                            table.addRow(new Object[]{msg.sender, msg.content, "Me", msgTime});
                            } catch (Exception ex) {
                            }
                        }   break;
                    case "login":
                        if (msg.content.equals("TRUE")) {
                            ui.jButton2.setEnabled(false);
                            // ui.jButton3.setEnabled(false);
                            ui.jButton4.setEnabled(true);
                            ui.jButton5.setEnabled(true);
                            ui.jTextArea1.append("[SERVER > Me] : Login Successful\n");
                            ui.jTextField3.setEnabled(false);
                            ui.jPasswordField1.setEnabled(false);
                        } else {
                            ui.jTextArea1.append("[SERVER > Me] : Login failed\n");
                        }   break;
                    case "test":
                        ui.jButton1.setEnabled(false);
                        ui.jButton2.setEnabled(true);
                        // ui.jButton3.setEnabled(true);
                        ui.jTextField3.setEnabled(true);
                        ui.jPasswordField1.setEnabled(true);
                        ui.jTextField1.setEditable(false);
                        ui.jTextField2.setEditable(false);
                        // ui.jButton7.setEnabled(true);
                        break;
                    case "newuser":
                        if (!msg.content.equals(ui.username)) {
                            boolean exists = false;
                            for (int i = 0; i < ui.model.getSize(); i++) {
                                if (ui.model.getElementAt(i).equals(msg.content)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                ui.model.addElement(msg.content);
                            }
                        }   break;
                    case "signup":
                        if (msg.content.equals("TRUE")) {
                            ui.jButton2.setEnabled(false);
                            //  ui.jButton3.setEnabled(false);
                            ui.jButton4.setEnabled(true);
                            ui.jButton5.setEnabled(true);
                            
                        } else {
                            ui.jTextArea1.append("[SERVER > Me] : Signup Failed\n");
                        }   break;
                    case "signout":
                        if (msg.content.equals(ui.username)) {
                            ui.jTextArea1.append("[" + msg.sender + " > Me] : Bye\n");
                            ui.jButton1.setEnabled(true);
                            ui.jButton4.setEnabled(false);
                            ui.jTextField1.setEditable(true);
                            ui.jTextField2.setEditable(true);
                            
                            for (int i = 1; i < ui.model.size(); i++) {
                                ui.model.removeElementAt(i);
                            }
                            
                            ui.clientThread.stop();
                        } else {
                            ui.model.removeElement(msg.content);
                            ui.jTextArea1.append("[" + msg.sender + " > All] : " + msg.content + " has signed out\n");
                        }   break;
                    case "upload_req":
                        if (JOptionPane.showConfirmDialog(ui, ("Accept '" + msg.content + "' from " + msg.sender + " ?")) == 0) {
                            
                            JFileChooser jf = new JFileChooser();
                            jf.setSelectedFile(new File(msg.content));
                            int returnVal = jf.showSaveDialog(ui);
                            
                            String saveTo = jf.getSelectedFile().getPath();
                            if (saveTo != null && returnVal == JFileChooser.APPROVE_OPTION) {
                                Download dwn = new Download(saveTo, ui);
                                Thread t = new Thread(dwn);
                                t.start();
                                //send(new Message("upload_res", (""+InetAddress.getLocalHost().getHostAddress()), (""+dwn.port), msg.sender));
                                send(new Message("upload_res", ui.username, ("" + dwn.port), msg.sender));
                            } else {
                                send(new Message("upload_res", ui.username, "NO", msg.sender));
                            }
                        } else {
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }   break;
                    case "upload_res":
                        if (!msg.content.equals("NO")) {
                            int port = Integer.parseInt(msg.content);
                            String addr = msg.sender;
                            
                            ui.jButton5.setEnabled(false);
                            ui.jButton6.setEnabled(false);
                            Upload upl = new Upload(addr, port, ui.file, ui);
                            Thread t = new Thread(upl);
                            t.start();
                        } else {
                            ui.jTextArea1.append("[SERVER > Me] : " + msg.sender + " rejected file request\n");
                        }   break;
                    default:
                        ui.jTextArea1.append("[SERVER > Me] : Unknown message type\n");
                        break;
                }
            } catch (IOException | ClassNotFoundException | HeadlessException | NumberFormatException ex) {
                keepRunning = false;
                ui.jTextArea1.append("[Application > Me] : Connection Failure\n");
                ui.jButton1.setEnabled(true);
                ui.jTextField1.setEditable(true);
                ui.jTextField2.setEditable(true);
                ui.jButton4.setEnabled(false);
                ui.jButton5.setEnabled(false);
                ui.jButton5.setEnabled(false);

                for (int i = 1; i < ui.model.size(); i++) {
                    ui.model.removeElementAt(i);
                }

                ui.clientThread.stop();

              //  System.out.println("Exception SocketClient run()");
            }
        }
    }

    public void send(Message msg) {
        try {
            Out.writeObject(msg);
            Out.flush();
            //System.out.println("Outgoing : " + msg.toString());

        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }

    public void closeThread(Thread t) {
        t = null;
    }
}
