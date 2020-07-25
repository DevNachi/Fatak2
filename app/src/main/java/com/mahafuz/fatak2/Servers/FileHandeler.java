package com.mahafuz.fatak2.Servers;
import android.util.Log;

import com.mahafuz.fatak2.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FileHandeler {
    Socket socket;
    DataInputStream bis;
    DataOutputStream dos;
    MainActivity activity;

    public FileHandeler(Socket socket, MainActivity activity) {
        Log.i(getClass().getCanonicalName(),"File Server Start");
        this.socket = socket;
        this.activity = activity;
        try {
            bis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        try {
            int i = 0;
            String fileName = bis.readUTF();
            String  fileSize = bis.readUTF();
            String extStore = System.getenv("EXTERNAL_STORAGE");
            File file = new File(extStore + "/Fatak", fileName);


            FileOutputStream fileInputStream = new FileOutputStream(file, true);
            byte[] buffer = new byte[256];
            int totalSize = 0;
            while ((i = bis.read(buffer, 0, buffer.length)) != -1) {
                fileInputStream.write(buffer, 0,i);
                totalSize+=i;
                System.out.println("chunk received "+totalSize);

            }
            activity.onThreadWorkDone(file.getPath() + "' file created with copied contents");
            System.out.println("\n'" + file.getPath() + "' file created with copied contents");
            fileInputStream.close();
            bis.close();
            socket.close();
        } catch (
                IOException e) {

        }
    }

    public void sentData(File file) throws IOException {
        FileSender fileSender = new FileSender(socket, file, activity);
        fileSender.execute();
    }
}