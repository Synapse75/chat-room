package org.example;

// Java implementation of Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;

// Server class
public class Server
{

    // Vector to store active clients
    static Vector<ClientHandler> ar = new Vector<>();
    static Set<String> ids = new HashSet<>();

    // counter for clients
    static int i = 0;

    public static void main(String[] args) throws IOException
    {
        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);

        Socket s;

        // running infinite loop for getting
        // client request
        while (true)
        {
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s,"client " + i, dis, dos);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.add(mtch);

            // start the thread.
            t.start();

            // increment i for new client.
            // i is used for naming only, and can be replaced
            // by any naming scheme
            i++;

        }
    }
}

// ClientHandler class
class ClientHandler implements Runnable
{
    private String lastReceivers = "";
    private String name;
    private String id;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;
    // constructor
    public ClientHandler(Socket s, String name,
                         DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
        this.id = generateId();
    }

    @Override
    public void run() {
        String received;
        try{
            //the newcomer can type his or her name and create a unique id for the newcomer
            dos.writeUTF("Welcome to the chat! \nPlease enter your username: ");
            String username = dis.readUTF();
            this.name = username;
            dos.writeUTF("Your ID is: " + id);
            //show the newcomer online users
            StringBuilder users = new StringBuilder("Online users:\n");
            for(ClientHandler mc:Server.ar){
                if(!mc.name.equals(this.name) && mc.isloggedin){
                    users.append(mc.name + "(" + mc.id + ")\n");
                }
            }
            dos.writeUTF("=============\n" + users.toString() + "\n=============");
            send(name + "(" + id + ") has joined the chat!");
            load(dos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try
            {
                // receive the string
                received = dis.readUTF();
                if (received.equals("[print-receiver]:the-message")) {
                    //users can see the receivers of the last messages by typing "[print-receiver]:the-message"
                    dos.writeUTF("Your last message was received by: \n" + lastReceivers);
                } else if (received.startsWith("#search")) {
                    //users can use "#search" to indicate they want to search for the messages with keyword in the chat history
                    String keyword = received.substring(8).trim();
                    String searchResult = search(keyword);
                    dos.writeUTF("Search results:\n" + searchResult);
                } else {
                    //modifiedMessage is a message with time stamp, name and ID
                    String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                    String modifiedMessage = "[" + timeStamp + "] " + this.name +"(" + id + ")" + " : " + received;
                    //save the message in the chat history
                    save(modifiedMessage);
                    //record the receivers of the user's last message
                    StringBuilder receivers = new StringBuilder();
                    for (ClientHandler mc : Server.ar) {
                        if (!mc.id.equals(this.id) && mc.isloggedin)
                        {
                            mc.dos.writeUTF(modifiedMessage);
                            receivers.append(mc.name + "(" + mc.id + ")\n");
                        }
                    }

                    if (receivers.length() > 0) {
                        lastReceivers = receivers.substring(0, receivers.length() - 1);
                    }
                }

            } catch(IOException e) {
                System.out.println(name + "(" + id + ") has disconnected.");
                break;
            }
        }
        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();
            Server.ar.remove(this);
            send(name + "(" + id + ") has left the chat!");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    //randomly generate user id
    private String generateId() {
        Random rand = new Random();
        String id;
        do {
            id = String.format("%05d", rand.nextInt(100000));
        } while(Server.ids.contains(id));
        Server.ids.add(id);
        return id;
    }
    //send the message to other online users
    private void send(String msg) {
        for (ClientHandler mc : Server.ar) {
            try {
                mc.dos.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //write the chat messages into a txt file
    private void save(String message) {
        try (FileWriter fw = new FileWriter("chat_log.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //when a new user come in, show him the chat history
    private void load(DataOutputStream dos) {
        File file = new File("chat_log.txt");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader("chat_log.txt"))) {
                String line;
                dos.writeUTF("==============================\nChat History:");
                while ((line = br.readLine()) != null) {
                    dos.writeUTF(line);
                }
                dos.writeUTF("==============================");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //user can search for messages by inputting a keyword
    private String search(String keyword) {
        StringBuilder results = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("chat_log.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(keyword)) {
                    results.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results.length() > 0 ? results.toString() : "No matching records found.";
    }

}