import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static logUtils.Logger.*;

public class TwitchConnect implements Runnable
{
    private DataInputStream is;

    private DataOutputStream os;

    // Basically a first in first out array list that is also thread safe :D
    private static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    // The client using the program. Contains the user name and OAUTH token.
    private final Client client;

    private String initialChannel = null;

    private static PipedOutputStream pipedOutputStream = new PipedOutputStream();

    private static PipedInputStream pipedInputStream = new PipedInputStream();

    private boolean acceptingMessages = true;

    public TwitchConnect(Client client) { this.client = client; }

    public TwitchConnect(Client client, String initialChannel)
    {
        this.client = client;
        this.initialChannel = initialChannel;
    }

    private final Thread messageSender = new Thread(() ->
    {
        log("messageSender running");
        while (! Thread.currentThread().isInterrupted())
        {
            try
            {
                // Don't spam twitch. It doesn't like it.
                Thread.sleep(777);
            } catch (InterruptedException y)
            {
                log(y.getMessage());
            }

            if (messages.size() < 1)
                continue;

            if (messages.peek() == null)
                continue;

            try
            {
                if (acceptingMessages)
                {
                    log("messageSender sending: " + messages.peek());
                    os.write(messages.poll().getBytes());
                    os.flush();
                }
            } catch (IOException y)
            {
                log(y.getMessage());
            }
        }
    });

    private final Thread messageProcessor = new Thread(() ->
    {
        DataInputStream inputStream = null;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try
        {
            pipedInputStream.connect(pipedOutputStream);
            inputStream = new DataInputStream(pipedInputStream);
        } catch(IOException e)
        {
            log(e.getMessage());
        }

        if (inputStream != null)
        {
            while (! Thread.currentThread().isInterrupted())
            {
                String data = null;
                try
                {
                    data = inputStream.readUTF();
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
                executor.execute(new DataProcessor(data));
            }
        }

    });

    public void run()
    {
        log("Starting messageReceiver service");
        // Connect
        connect();

        // Login
        logIn();

        // Enable TwitchAPI options
        twitchAPIOps();

        // Start message Sender Thread
        log("Starting messageSender service");
        messageSender.start();
        log("Starting messageProcessor service");
        messageProcessor.start();

        DataOutputStream outputStream = new DataOutputStream(pipedOutputStream);

        log("messageReceiver service running");
        while (! Thread.currentThread().isInterrupted())
        {
            // Don't add the received data directly to the StringProperty.
            // Check it for relevance before adding.
            String tmpData = String.valueOf(BasicIO.readLine(is));

            if (tmpData.substring(0, 4).equals("PING"))
            {
                sendMessage("PONG " + tmpData.substring(5));
            }
            else if (tmpData.equals(":tmi.twitch.tv NOTICE * :Login authentication failed"))
            {
                acceptingMessages = false;
                try
                {
                    outputStream.writeUTF("EEE: Incorrect login information!");
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
                messages.clear();
            }
            else
            {
                try
                {
                    outputStream.writeUTF(tmpData);
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
            }
        }

        if (Thread.currentThread().isInterrupted())
        {
            messageSender.interrupt();
            messageProcessor.interrupt();
        }
    }

    private void connect()
    {
        try
        {
            log("Connecting to twitch IRC services");
            Socket socket = new Socket(TwitchConnectionInfo.getIrcChatTwitchTv(), TwitchConnectionInfo.getPort());
            WildChat.connected = true;
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            log("Connection started");
        } catch(IOException e)
        {
            WildChat.connected = false;
            log(e.getMessage());
            log("Failed to connect to twitch IRC services");
        }
    }

    private void logIn()
    {
        log("Sending client credentials");
        // None of this actually happens until the messageSender is started
        sendMessage("PASS oauth:" + client.getOauth());
        sendMessage("NICK " + client.getNick());
    }

    private void twitchAPIOps()
    {
        log("Requesting advanced operations from twitch IRC");
        // None of this actually happens until the messenger services is started
        sendMessage("CAP REQ :twitch.tv/membership");
        sendMessage("CAP REQ :twitch.tv/tags");
        sendMessage("CAP REQ :twitch.tv/commands");
        if (initialChannel != null)
        {
            sendMessage("JOIN " + initialChannel);
            Platform.runLater(() ->
            {
                WildChat.session.setChannel(initialChannel);
                WildChat.displayMessage("> Joining channel " + initialChannel + "...");
            });
        }
    }

    // Send a message to the Twitch IRC
    public synchronized void sendMessage(String command)
    {

        if (acceptingMessages)
        {
            try
            {
                messages.put(command + "\r\n");
            } catch (InterruptedException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
}
