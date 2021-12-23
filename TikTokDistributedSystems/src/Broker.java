
import softeng.aueb.tiktok.VideoFile;

import java.io.*;
import java.net.*;
import java.util.*;


@SuppressWarnings("all")
public class Broker extends Node {
    private final String GLOBALIP = "192.168.1.4";
    public ArrayList<String> brokerhashtag = new ArrayList<>();
    public ArrayList<String> brokerchannelnameslist = new ArrayList<>();
    public ArrayList<softeng.aueb.tiktok.VideoFile> VideosPublisherConnection = new ArrayList<>();

    public Map<String,ArrayList<String>> subs = new HashMap<>();
    public Util.Pair<String,Integer> contact;
    public Map<String,Util.Pair<String,Integer>> ChannelServerInfo = new HashMap<>();
    public volatile HashMap<String,ArrayList<softeng.aueb.tiktok.VideoFile>> channelContent = new HashMap<>();
    public Map<String,Util.Pair<String,Integer>> VideoOwnerConnection = new HashMap<>();
    public Map<Integer,Util.Pair<String, Integer>> ListOfBrokers = new TreeMap<>();
    public HashSet<String> hashTags = new HashSet<>();
    public HashSet<String> channels = new HashSet<>();

    public HashMap<Integer,HashSet<String>> fellowBrokersInfo = new HashMap<>();

    String directory;
    String channelname;
    InetAddress owneraddress;
    int ownerport;
    InetAddress ipaddress;
    int hashid;
    int port;
    //public ArrayList<AppNodes> registeredAppNodes = new ArrayList<>();

    public Broker(){
    }

    @Override
    public String toString() {
        return "Broker{" +
                "brokerhashtag=" + brokerhashtag +
                ", ipaddress=" + ipaddress +
                ", hashid=" + hashid +
                ", port=" + port +
                '}';
    }

    public static void main(String[] args) {
        try {
            Broker broker = new Broker();
            broker.init(args[0],args[1],args[2],args[3]);
            broker.ChannelServerInfo.put("test",new Util.Pair<String,Integer>("localhost",7000));
            broker.channels.add("ody");
            broker.run();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void run() throws UnknownHostException {
        ServerSocket serverSocket = null;
        Handler handler;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Port is now open" + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client has connected");
                System.out.println("Connection received from " + socket.getInetAddress().getHostAddress() + " : " + socket.getPort());
                handler = new Handler(socket);
                handler.start();
            }
            //handler.join();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private class Handler extends Thread {
        private boolean exit = false;
        private Socket conn;
        public ObjectOutputStream oos;
        public ObjectInputStream ois;

        public Handler(Socket conn) throws IOException {
            this.conn = conn;
            oos = new ObjectOutputStream(conn.getOutputStream());
            ois = new ObjectInputStream(conn.getInputStream());
        }

        public void run(){
            try {
                while (!exit) {

                    byte choice = ois.readByte(); //we take the choice
                    switch (choice) {
                        case 1: //register
                            String channelName = (String) ois.readObject();
                            System.out.println(channelName);
                            int channelHash = Util.getModMd5(channelName);
                            channelHash /= 3;
                            System.out.println(hashid + "====" + channelHash);
                            for (int brokerID : ListOfBrokers.keySet()) {
                                System.out.println(hashid + "====" + brokerID);
                                if (channelHash < brokerID && brokerID == hashid) {
                                    oos.writeBoolean(true);
                                    oos.flush();
                                    System.out.println("Adding to list the Channel name");
                                    brokerchannelnameslist.add(channelName);
                                    System.out.println(ListOfBrokers.keySet().toString());
                                    //oos.writeObject(ListOfBrokers);
                                    //oos.flush();
                                    break;
                                } else if (channelHash < brokerID) {
                                    System.out.println("To the next Broker");
                                    oos.writeBoolean(false);
                                    oos.flush();
                                    oos.writeObject(ListOfBrokers.get(brokerID).item2);
                                    System.out.println(ListOfBrokers.get(brokerID));
                                    oos.flush();
                                    _stop();
                                    System.out.println("AppNode has left the broker");
                                    break;

                                } else continue;
                            }
                            System.out.println(conn.getInetAddress().getHostAddress());
                            ChannelServerInfo.put(channelName,new Util.Pair<String,Integer>("127.0.0.1",7000));
                            channels.add(channelName);
                            break;
                        case 2: //publish a video
                            Util.debug("Reading video");
                            softeng.aueb.tiktok.VideoFile video = (VideoFile) ois.readObject();
                            VideosPublisherConnection.add(video);

                            String hashtag = (String) ois.readObject();

                            channelName = video.getChannelName();

                            video.setPath(directory+"/"+video.getVideoName());

                            boolean contains = channelContent.containsKey(channelName);

                            boolean contains_ = channelContent.containsKey(hashtag);

                            System.out.println("Starting to add channel content");

                            if (!contains) {
                                channelContent.put(channelName, new ArrayList<VideoFile>());
                                System.out.println("created arraylist channel");
                            }
                            if (!contains_) {
                                channelContent.put(hashtag, new ArrayList<VideoFile>());
                                System.out.println("created arraylist hashtag");

                            }

                            channelContent.get(channelName).add(video);
                            System.out.println("added to arraylist channel");


                            channelContent.get(hashtag).add(video);
                            System.out.println("added to arraylist hashtag");

                            hashTags.add(hashtag);

                            Util.debug("Added hashtag to broker's hashtag");
                            FileOutputStream out = new FileOutputStream(directory+"/"+video.getVideoName());
                            Util.debug(directory+"/"+video.getVideoName());
                            byte[] bytes = new byte[512];
                            try {
                                for (;;) {
                                    bytes = (byte[]) ois.readObject();
                                    if (bytes == null){
                                        break;
                                    }
                                    out.write(bytes);
                                }
                            }catch (EOFException exc){
                                System.out.println("Ton pairneis");
                            }

                            System.out.println(channelContent.toString());
                            Util.debug("file donwloaded");
                            out.close();
                            Util.debug("file closed");
                            System.out.println("Finished video receiving");
                            pushToAllSubs(video,channelName);
                            pushToAllSubs(video,hashtag);
                            _stop();
                            break;

                        /*case 8:
                            *//** Process to send videos to subscribers  **//*
                            channelName = (String) ois.readObject();
                            Iterator it = subs.entrySet().iterator();
                            while (it.hasNext()){
                                Map.Entry pair = (Map.Entry) it.next();
                                String subbed = (String) pair.getKey();
                                ArrayList<String> info = (ArrayList<String>) pair.getValue();
                                Util.Pair<String,Integer> contactInfo;
                                if (ChannelServerInfo.containsKey(channelName)){
                                    contactInfo = ChannelServerInfo.get(channelName);
                                }
                                else continue;
                                Socket subscriber = new Socket(contactInfo.item1,contactInfo.item2);
                                ObjectOutputStream outSub = new ObjectOutputStream(subscriber.getOutputStream());
                                ObjectInputStream inSub = new ObjectInputStream(subscriber.getInputStream());
                                File folder = new File(""+directory);
                                File[] listOfFiles = folder.listFiles();

                                for (File file : listOfFiles) {
                                    if (file.isFile()) {
                                        byte[] fileData = Util.loadVideoFromDiskToRam(file.getName());
                                        List<byte[]> chunckedData = Util.chunkifyFile(fileData);
                                        for (byte[] buffer : chunckedData){
                                            outSub.write(buffer);
                                        }
                                        outSub.flush();
                                    }
                                }
                             }

                            break;*/


                        case 9:
                            String name =(String) ois.readObject();
                            //appip = (String) ois.readObject();
                            int apport = (int) ois.readObject();
                            ChannelServerInfo.computeIfAbsent(name,k->new Util.Pair<String,Integer>("",0));
                            //ChannelServerInfo.put(name,new Util.Pair<String,Integer>(appip, apport));
                            System.out.println("Inserted contact information");




                            break;
                        case 3: //returns hashtags and channelnames
                            byte action = ois.readByte();
                            channelname = (String) ois.readObject();

                            if (action == 1) {
                                ArrayList<String> hashtagnames = new ArrayList<>();
                                for (String s : hashTags) {
                                    hashtagnames.add(s);
                                }
                                oos.writeObject(hashtagnames);
                                oos.flush();
                            } else {
                                ArrayList<String> channelnames = new ArrayList<>();
                                for (String s : channels) {
                                    if(!s.equals(channelname)) {
                                        channelnames.add(s);
                                    }
                                }
                                oos.writeObject(channelnames);
                                oos.flush();
                            }


                            break;
                        case 4: //subscribe customer to a channel

                            channelname = (String) ois.readObject();

                            ArrayList<String> channelnames = new ArrayList<>();
                            for (String s : channels) {
                                if(!s.equals(channelname)) {
                                    channelnames.add(s);
                                }
                            }
                            oos.writeObject(channelnames);
                            oos.flush();
                            break;

                        case 7: //gets the channelname that AppNode wants to subscribe
                            String channame = (String) ois.readObject(); //channelname giving the order
                            String subchannel = (String) ois.readObject(); //channel that we want to sub
                            System.out.println(subchannel);
                            boolean contains2 = subs.containsKey(subchannel);

                            if(channels.contains(subchannel)) {
                                if (!contains2) {
                                    subs.put(subchannel, new ArrayList<String>());
                                }
                                subs.get(subchannel).add(channame);
                                System.out.println("Subscribition complete");
                                System.out.println(subs);
                            }else if(hashTags.contains(subchannel)) {
                                if (!contains2) {
                                    subs.put(subchannel, new ArrayList<String>());
                                }
                                subs.get(subchannel).add(channame);
                                System.out.println("Subscribition complete");
                                System.out.println(subs);

                            }
                            else{
                                System.out.println("An error has occured");
                            }
                            System.out.println(channame);
                            Util.Pair<String,Integer> infos = ChannelServerInfo.get(channame);
                            System.out.println("ip: "+infos.item1);
                            System.out.println("port "+infos.item2);
                            pushToSub(subchannel,infos.item1, infos.item2);
                            break;

                        case 5: //process of returning channels or hashtags requested by AppNodes

                            String desired = (String) ois.readObject();
                            channelname = (String) ois.readObject();
                            HashSet<String> interestingvideos = new HashSet<>();
                            for (VideoFile v : VideosPublisherConnection) {
                                if ((v.getChannelName().equals(desired) || v.associatedHashtags.contains(desired))
                                        && !v.getChannelName().equals(channelname)) {
                                    //checks if we want this video
                                    interestingvideos.add(v.getVideoName());

                                }

                            }
                            System.out.println(interestingvideos);
                            oos.writeObject(interestingvideos);
                            oos.flush();


                            break;


                        case 6: // process to send the video
                            String filename = (String) ois.readObject();
                            channelname = (String) ois.readObject();
                            boolean exists = false;

                            for(VideoFile v: VideosPublisherConnection){
                                if(v.videoName.equals(filename) && !v.getChannelName().equals(channelname)){
                                    //checks if video exists and if the publisher is the AppNode asking
                                    exists=true;
                                    contact = VideoOwnerConnection.get(v.videoName);
                                    System.out.println("Found file");
                                     break;
                                }
                            }
                            oos.writeBoolean(exists);
                            oos.flush();

                            oos.writeObject(contact);
                            oos.flush();
                            break;
                    }
                }
            }
            catch (EOFException e){
                System.out.println("EOFException");
            }
            catch(ClassNotFoundException | IOException e){
                e.printStackTrace();
            }
            finally {
                try {
                    ois.close();
                    oos.close();
                    conn.close();
                    //  System.exit(1);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        public void _stop()
        {
            exit = true;
        }


        public void pushToAllSubs(VideoFile videoFile, String contentCreator) throws IOException, ClassNotFoundException {

            ArrayList<String> contentSubs = subs.get(contentCreator);
            if (contentSubs == null){
                System.out.println("NULL");
                return;
            }
            for (String subsciber : contentSubs){
                String ip = ChannelServerInfo.get(subsciber).item1;
                int port = ChannelServerInfo.get(subsciber).item2;

                Socket subSocket = new Socket(ip,port);
                /*if (subSocket.getInetAddress().isReachable(5000)){
                    System.out.println("Subscriber not connected");
                    continue;
                }*/
                ObjectOutputStream outSub = new ObjectOutputStream(subSocket.getOutputStream());
                ObjectInputStream inSub = new ObjectInputStream(subSocket.getInputStream());
                outSub.writeByte(1);
                outSub.flush();
                System.out.println("SENT BYTE");

                outSub.writeObject(videoFile);
                outSub.flush();
                System.out.println("SENT VIDEO");
                byte[] fileData = Util.loadVideoFromDiskToRam(videoFile.path);
                System.out.println("LOAD VIDEO");
                List<byte[]> chunckedData = Util.chunkifyFile(fileData);
                for (byte[] buffer : chunckedData){
                    outSub.writeObject(buffer);
                    outSub.flush();
                }
                outSub.writeObject(null);
                outSub.flush();
                outSub.writeByte(0);
                outSub.flush();
                System.out.println("Sent video to sub");
            }
        }

        public void pushToSub(String creator,String ip, int port) throws IOException, ClassNotFoundException{
            System.out.println("ip in func "+ip);
            System.out.println("port in func "+port);
            Socket subscriber = new Socket(ip,port);
            ObjectOutputStream outSub = new ObjectOutputStream(subscriber.getOutputStream());
            System.out.println("out");
            ObjectInputStream inSub = new ObjectInputStream(subscriber.getInputStream());
            System.out.println("in");
            System.out.println(channelContent);
            ArrayList<VideoFile> videos = channelContent.get(creator);

            if (videos == null){
                System.out.println("NULL");
                outSub.writeByte(0);
                outSub.flush();
                return;
            }
            if (videos.isEmpty()){
                System.out.println("EMPTY");
                outSub.writeByte(0);
                outSub.flush();
                return;
            }

            for (VideoFile video : videos){

                outSub.writeByte(1);
                outSub.flush();
                System.out.println("SENT BYTE");

                outSub.writeObject(video);
                outSub.flush();
                System.out.println("SENT VIDEO");
                byte[] fileData = Util.loadVideoFromDiskToRam(video.path);
                System.out.println("LOAD VIDEO");
                List<byte[]> chunckedData = Util.chunkifyFile(fileData);
                for (byte[] buffer : chunckedData){
                    outSub.writeObject(buffer);
                    outSub.flush();
                }
                outSub.writeObject(null);
                outSub.flush();
            }
            outSub.writeByte(0);
            outSub.flush();
        }

    }
    public void init(String ip,String port, String pathname,String directory) throws UnknownHostException {
        hashid = Util.getModMd5(ip+","+port);
        this.directory = directory;
        this.port = Integer.parseInt(port);
        this.ipaddress = InetAddress.getByName(ip);
        ListOfBrokers.put(hashid,new Util.Pair<String,Integer>(ip,this.port));
        try {
            File f = new File("brokers/" + pathname);
            Scanner myReader = new Scanner(f);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                int otherHash = Util.getModMd5(data);
                String[] data2 = data.split(",");
                int brokerport = Integer.parseInt(data2[1]);
                String brokerip = data2[0];
                ListOfBrokers.put(otherHash,new Util.Pair<>(brokerip,brokerport));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/*    public void update() {
        try {
            fellowBrokersInfo.clear();
            for (Util.Pair<String, Integer> broker_info : ListOfBrokers) {
                Socket fellob = new Socket(broker_info.item1, broker_info.item2);
                ObjectInputStream ois = new ObjectInputStream(fellob.getInputStream());
                HashSet<String> brokerHash = (HashSet<String>) ois.readObject();
                int id = Util.getModMd5(broker_info.item1 + broker_info.item2.toString());
                fellowBrokersInfo.put(id, brokerHash);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }*/

    public ArrayList<VideoFile> getVideos() {
        return VideosPublisherConnection;
    }

    public ArrayList<String> getBrokerchannelnameslist() {
        return brokerchannelnameslist;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getBrokerIP(){
        return ipaddress;
    }
    public int getHashID(){return hashid;}
    //public ArrayList<AppNodes> getRegisteredAppNodes(){ return registeredAppNodes; }
    public ArrayList<String> getBrokerhashtag(){return brokerhashtag;}

}


