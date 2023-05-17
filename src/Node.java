import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

public class Node {
    public PublicKey pubKey;
    public PrivateKey priKey;
    public static String address;
    public int port;
    public final ArrayList<Integer> peerList = new ArrayList<Integer>();

    public Node(int port) throws Exception {
        this.port = port;
        generateKeyPair();
        new Thread(() -> {
            try {
                openServer();
            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                System.err.println("Fail to open Server. Reason: " + e );
            }
        }).start();
        new Thread(() -> {
            try {
                setPeerList();
            } catch (IOException | InterruptedException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                System.err.println("Fail to set peer list. Reason: " + e );
            }
        }).start();

    }

    public void addGenesisBlock() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (port == 3000){
            //Mining blocks
            Block bk1 = Miner.findBlock(BlockChain.getPreviousIndex() + 1, BlockChain.getPreviousHash(), new Date().getTime() / 1000, new ArrayList<Transaction>(), BlockChain.getDifficulty(), address,false);

            Boolean added = BlockChain.addBlock(bk1);

            if (added) {
                for (int port : peerList) {
                    sendBlock(port, bk1);
                }
            }
        }
    }

    public void generateKeyPair() throws Exception{
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        gen.initialize(256, random);
        KeyPair keyPair = gen.generateKeyPair();
        pubKey =  keyPair.getPublic();
        priKey =  keyPair.getPrivate();
        address = Base64.getEncoder().encodeToString(pubKey.getEncoded());

    }

    public void setPeerList() throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        boolean getBKforFirstTime = true;
        while(true) {
            File file = new File("peerList.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                int portNo = Integer.parseInt(scanner.nextLine());
                if (portNo != port) {
                    synchronized (peerList) {
                        if (!peerList.contains(portNo)){
                            peerList.add(portNo);
                        }
                    }
                }
            }

            if(getBKforFirstTime){
                addGenesisBlock();
                for(int port: peerList){
                    System.out.println("get blocks from " + port);
                    getBlock(port);
                }
                getBKforFirstTime = false;
            }


            Thread.sleep(5000);
        }
    }

    public void openServer() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileWriter fw = new FileWriter("peerList.txt", true);
        fw.write(port + "\n");
        fw.close();

        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            Socket socket = serverSocket.accept();
            ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
         //   System.out.println("received message");
            ArrayList<Object> inList = (ArrayList<Object>)objIn.readObject();
            ArrayList <Block> blkList = new ArrayList<Block>();
            ArrayList<Transaction> transactions = new ArrayList<Transaction>();
            if(inList.size()>1) {
                System.out.println("Receviced Data server:" + inList.get(1).getClass().getSimpleName());
                System.out.println("List size: " + inList.size());
                if(inList.get(1).getClass().getSimpleName().toString()=="Transaction"){
                    inList.forEach(data->{
                        transactions.add((Transaction) data);
                    });
                } else {
                    inList.forEach(data -> {
                        blkList.add((Block) data);
                    });
                }
            }
            if (blkList.size() == 0){
                synchronized (BlockChain.blockchain) {
                    ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
                    objOut.writeObject(BlockChain.blockchain);
            //        System.out.println("Have sent the Blocks");
                }
            }else if (blkList.size() == 2 && blkList.get(0) == null){
                synchronized (BlockChain.blockchain) {
                    if(BlockChain.blockchain.size() == 0 && BlockChain.isValidGenesisBlock(blkList.get(1))){
                        BlockChain.blockchain.add(blkList.get(1));
                        BlockChain.updateUTXOs(blkList.get(1));
                    }else if (BlockChain.isValidNewBlock(BlockChain.getPreviousBlock(), blkList.get(1))) {
                        BlockChain.blockchain.add(blkList.get(1));
                        BlockChain.updateUTXOs(blkList.get(1));
                    }
                    // If block added = transaction recorded, clear stored transaction
                    Main.storgedTransactions.clear();
                }
            }

            if(transactions.size()>0){
                Main.storgedTransactions.add(transactions.get(1));
            }
        }
    }


    public void sendBlock(int portNo, Block block) throws IOException {
        Socket socket = new Socket("127.0.0.1", portNo);
        OutputStream out = socket.getOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        ArrayList<Block> bkList = new ArrayList<Block>();
        bkList.add(null);
        bkList.add(block);
        objOut.writeObject(bkList);

    }

    public void getBlock(int portNo) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        Socket socket = new Socket("127.0.0.1", portNo);
        OutputStream out = socket.getOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        ArrayList<Block> bkList = new ArrayList<Block>();
        objOut.writeObject(bkList);

        ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
       // System.out.println("received message");
        ArrayList<Block> blkList = (ArrayList<Block>) objIn.readObject();
        //System.out.println("List size: " + blkList.size());

        synchronized (BlockChain.blockchain) {
            if (BlockChain.isValidChain(blkList) && blkList.size() > BlockChain.blockchain.size()) {
                for(int i =BlockChain.blockchain.size(); i < blkList.size(); i++){
                        BlockChain.blockchain.add(blkList.get(i));
                        BlockChain.updateUTXOs(blkList.get(i));
                }
            }
        }
        socket.close();
    }

    public void sendStagedTransactions(int portNo, Transaction transaction) throws IOException {
        Socket socket = new Socket("127.0.0.1", portNo);
        OutputStream out = socket.getOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(null);
        transactions.add(transaction);
        objOut.writeObject(transactions);
    }

    public void getStagedTransactions(int portNo) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        Socket socket = new Socket("127.0.0.1", portNo);
        OutputStream out = socket.getOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        objOut.writeObject(transactions);
        ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
         ArrayList<Transaction> stagedTransaction  = (ArrayList<Transaction>) objIn.readObject();
         if(stagedTransaction.size()<2){
                return;
         }
        synchronized (Main.storgedTransactions) {
            Main.storgedTransactions.add(stagedTransaction.get(1));
        }
        socket.close();
    }


    public int getPort() {
        return port;
    }

    public PrivateKey getPriKey() {
        return priKey;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public ArrayList<Integer> getPeerList() {
        return peerList;
    }

    public static String getAddress() {
        return address;
    }
}
