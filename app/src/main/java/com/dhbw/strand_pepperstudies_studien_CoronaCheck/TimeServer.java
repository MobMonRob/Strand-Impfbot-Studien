package com.dhbw.strand_pepperstudies_studien_CoronaCheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer {
    int port = 1755;
    String vacStatus = "";

    public String run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);
            boolean runThread = true;

            while (runThread) {
                Socket socket = serverSocket.accept();

                System.out.println("New client connected");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                vacStatus = reader.readLine();

                System.out.println("InfoTImeServer " + vacStatus);

                if (vacStatus.equals("true") || vacStatus.equals("false")) {
                    runThread = false;
                    System.out.println(runThread);
                    socket.close();
                    return vacStatus;
                }

            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();

        }
        return vacStatus;
    }
}