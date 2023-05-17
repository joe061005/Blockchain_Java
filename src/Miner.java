import java.util.ArrayList;

public class Miner {

    public static boolean hashMatchesDifficulty(String hash, int difficulty){
        String hashInBinary = hexToBin(hash);
        String requiredPrefix = "0".repeat(difficulty);
        return hashInBinary.startsWith(requiredPrefix);

    }
    //Mining: implement a Proof-of-Work algorithm.
    public static Block findBlock(int index, String previousHash, long timestamp,  ArrayList<Transaction> transactions, int difficulty, String address, boolean isTransaction){
        //start nonce from 0
        int nonce = 0;
        while (true){
            if(!isTransaction) {
                ArrayList<Transaction> txList = new ArrayList<Transaction>();
                txList.add(new Transaction(address, index));
                // Genesis Block no need add transaction list
                // txList.add(transaction);
                //Calculate the SHA-256 hash value of all the information.
                String hash = Block.sha256(Integer.toString(index) + previousHash + Long.toString(timestamp) + getTxString(txList) + Integer.toString(difficulty) + Integer.toString(nonce));
                if (hashMatchesDifficulty(hash, difficulty)) {
                    //If the output is under the target, add the new block to the blockchain.
                    return new Block(index, hash, previousHash, timestamp, txList, difficulty, nonce);
                }
                //Otherwise, increment nonce by 1 and repeat step c)
                nonce++;
            }else{
                ArrayList<Transaction> txList = new ArrayList<Transaction>();
                txList.add(new Transaction(address, index));
                transactions.forEach(transaction -> {
                    txList.add(transaction);
                });
                //
                String hash = Block.sha256(Integer.toString(index) + previousHash + Long.toString(timestamp) + getTxString(txList) + Integer.toString(difficulty) + Integer.toString(nonce));
                if (hashMatchesDifficulty(hash, difficulty)) {
                    System.out.println("This is Transaction!");


                    return new Block(index, hash, previousHash, timestamp, txList, difficulty, nonce);
                }
                nonce++;
            }
        }
    }

    public static String getTxString(ArrayList<Transaction> txList){
        if(txList.size()<1) {
            return txList.get(0).id + txList.get(1).id;
        }else{
            return txList.get(0).id;
        }
    }

    public static String hexToBin(String hex){
        hex = hex.replaceAll("0", "0000");
        hex = hex.replaceAll("1", "0001");
        hex = hex.replaceAll("2", "0010");
        hex = hex.replaceAll("3", "0011");
        hex = hex.replaceAll("4", "0100");
        hex = hex.replaceAll("5", "0101");
        hex = hex.replaceAll("6", "0110");
        hex = hex.replaceAll("7", "0111");
        hex = hex.replaceAll("8", "1000");
        hex = hex.replaceAll("9", "1001");
        hex = hex.replaceAll("a", "1010");
        hex = hex.replaceAll("b", "1011");
        hex = hex.replaceAll("c", "1100");
        hex = hex.replaceAll("d", "1101");
        hex = hex.replaceAll("e", "1110");
        hex = hex.replaceAll("f", "1111");
        return hex;
    }
}
