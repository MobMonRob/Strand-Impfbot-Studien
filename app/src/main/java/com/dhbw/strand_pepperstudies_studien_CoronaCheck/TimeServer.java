package com.dhbw.strand_pepperstudies_studien_CoronaCheck;
import java.io.*;
import java.net.*;
import java.util.Date;

/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class TimeServer {
        int port = 1755;
        String vacStatus = "";

        public String run(){
            try (ServerSocket serverSocket = new ServerSocket(port)) {

                System.out.println("Server is listening on port " + port);
                boolean test = true;

                while (test) {
                    Socket socket = serverSocket.accept();

                    System.out.println("New client connected");

                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    vacStatus = reader.readLine();

                    System.out.println("InfoTImeServer " + vacStatus);
                    System.out.println(test);
                    if (vacStatus.equals("true") || vacStatus.equals("false")){
                        test = false;
                        System.out.println(test);
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