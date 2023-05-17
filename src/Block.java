import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.io.Serializable;

public class Block implements Serializable{
    public int index;
    public long timestamp;
    public String hash;
    public String previousHash;
    public ArrayList<Transaction> data;
    public int difficulty;
    public int nonce;

    public Block(int index, String hash, String previousHash, long timestamp, ArrayList<Transaction> data, int difficulty, int nonce){
        this.index = index;
        this.hash = hash;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.data = data;
        this.difficulty = difficulty;
        this.nonce = nonce;
    }


    /*/Part 2 - Mining part
    public String calculateHash(){
        //Combine all function in the block and calculate the sha-256 hash value
        String sha256Data = (this.index+this.previousHash+this.timestamp+this.data+this.nonce);
        return sha256(sha256Data);
    }
    public void mineBlock(int difficulty){
        String[] ary = new String [difficulty+1];
        String joinAry =  String.join("0",ary);
        while(this.hash.substring(0,difficulty)!= joinAry){
            //nonce++ and repeat
            this.nonce++;
            this.hash = this.calculateHash();
            System.out.println("Under the target!");
        }
        System.out.println("Block mined: " +  this.hash);
    }
    */

    public static String sha256(String data){
        try{
            MessageDigest algo = MessageDigest.getInstance("SHA-256");
            algo.update(data.getBytes());
            byte[] hashOfData = algo.digest();
            return byteToHex(hashOfData);

        }catch (NoSuchAlgorithmException ex){
            return "";
        }
    }

    private static String byteToHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
            return String.format("%0" + paddingLength + "d", 0) + hex;
        else
            return hex;
    }

    public String toString(){
        String returnString =
         "Index: " + index
                + "\nTimestamp: " + timestamp
                + "\nHash: " + hash
                + "\nPrevious hash: " + previousHash
                + "\nCoinbase transaction: " + data.get(0);
            if(data.size()>1){
                returnString += "\nRegular transaction: ";
                for(int i =1; i< data.size();i++){
                    returnString +="\n"+i+": "+data.get(i);
                }
            }

                 returnString +=
                 "\nDifficulty: " + difficulty
                + "\nNonce: " + nonce
                + "\nBinary hash: " + Miner.hexToBin(hash)
                + "\n";

        return returnString;
    }

    public ArrayList<Transaction> getData() {
        return data;
    }

    public String getPreviousHash() {
        return previousHash;
    }
}
