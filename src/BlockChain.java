import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class BlockChain {

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static ArrayList<Transaction> UTXOs = new ArrayList<Transaction>();


    public static Boolean addBlock(Block block) throws NoSuchAlgorithmException, InvalidKeySpecException {
        System.out.println("Blockchain size: " + blockchain.size());

        if ((blockchain.size() == 0 && isValidGenesisBlock(block))){
            System.out.println("Block is the Genesis Block");
            System.out.println("Block mined!");
            System.out.println("Difficulty: " + block.difficulty+"\n"+
                    "binarayHash: " + Miner.hexToBin(block.hash)+"\n"+
                    "hash: " + block.hash+"\n"+
                    "nonce: " + block.nonce);
            blockchain.add(block);
            updateUTXOs(block);
            return true;
        }
        //Block is the new block
        else if(isValidNewBlock(blockchain.get(blockchain.size() - 1), block)){
            System.out.println("Block is the New Block");
            System.out.println("Block mined!");
            System.out.println("Difficulty: " + block.difficulty+"\n"+
                    "binarayHash: " + Miner.hexToBin(block.hash)+"\n"+
                    "hash: " + block.hash);
            blockchain.add(block);
            updateUTXOs(block);
            return true;
        }
        else {

            System.out.println("Invalid block");
            return false;
        }
    }

    public static void updateUTXOs(Block blk){
        ArrayList<Transaction> txData = blk.getData();
        if(blk.getPreviousHash().equals("0")){
            handleTxOutput(txData.get(0));
        }else {
//            handleTxOutput(txData.get(0));
//            handleTxOutput(txData.get(1));
            for(Transaction tx: txData){
                handleTxOutput(tx);
            }
        }
    }


    public static void handleTxOutput(Transaction tx){
        String address = tx.getTxOut().getAddress();
        if(address.equals(Node.getAddress())){
            UTXOs.add(tx);
        }
    }

    public static Boolean isValidChain(ArrayList<Block> blockchain) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Block currentBlock;
        Block previousBlock;

        for(int i = 1; i < blockchain.size(); i++){
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            if(i - 1 == 0){
                if(!isValidGenesisBlock(previousBlock)){
                    return false;
                }else if (!isValidBlock(previousBlock, currentBlock)){
                    return false;
                }
            }else{
                if(!isValidBlock(previousBlock, currentBlock)){
                    return false;
                }
            }
        }
        return true;
    }

    public static Block getPreviousBlock(){
        return blockchain.size() == 0? null : blockchain.get(blockchain.size() - 1);
    }

    public static int getPreviousIndex(){
        return blockchain.size() == 0? -1 : blockchain.get(blockchain.size() - 1).index;
    }

    public static String getPreviousHash(){
        return blockchain.size() == 0? "0" : blockchain.get(blockchain.size() - 1).hash;
    }

    public static Boolean isValidNewBlock(Block prevBlock, Block newBlock) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (prevBlock.index + 1 != newBlock.index){
            return false;
        }else if (!prevBlock.hash.equals(newBlock.previousHash)){
            return false;
        }else if (!calcHash(newBlock).equals(newBlock.hash)){
            return false;
        }else if (!Miner.hashMatchesDifficulty(newBlock.hash, newBlock.difficulty)){
            return false;
        }else if(!validUTXO(newBlock)){
            return false;
        }
        return true;
    }

    public static Boolean isValidBlock(Block prevBlock, Block newBlock){
        if (prevBlock.index + 1 != newBlock.index){
            return false;
        }else if (!prevBlock.hash.equals(newBlock.previousHash)){
            return false;
        }else if (!calcHash(newBlock).equals(newBlock.hash)){
            return false;
        }else if (!Miner.hashMatchesDifficulty(newBlock.hash, newBlock.difficulty)){
            return false;
        }
        return true;
    }

    public static boolean validUTXO(Block newBlock) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String txID = newBlock.getData().get(1).getTxIn().getTxOutId();
        String signature = newBlock.getData().get(1).getTxIn().getSignature();
        for(Block blk: BlockChain.blockchain){
            if(blk.getData().get(0).getId().equals(txID)){
                return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(0).getTxOut().getAddress()), signature, "owner");
              }
//            else if (blk.getData().get(1).getId().equals(txID)){
//                return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(1).getTxOut().getAddress()), signature, "owner");
//            }
            else if (blk.getData().size()>1){
                for(int i=1;i<blk.getData().size();i++){
                    if (blk.getData().get(i).getId().equals(txID)) {
                        return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(i).getTxOut().getAddress()), signature, "owner");
                    }
                }
            }
        }
        return false;
    }

    public static Boolean isValidGenesisBlock(Block block){
        if(!calcHash(block).equals(block.hash) || !Miner.hashMatchesDifficulty(block.hash, block.difficulty)){
            return false;
        }
        return true;
    }

    public static String calcHash(Block bk){
        return Block.sha256(Integer.toString(bk.index) + bk.previousHash + Long.toString(bk.timestamp) + Miner.getTxString(bk.data) + Integer.toString(bk.difficulty) + Integer.toString(bk.nonce) );
    }

    public static int getDifficulty(){
        if (blockchain.size() == 0){
            return 5;
        }
        Block latestBlock = getPreviousBlock();
        if (latestBlock.index % 10 == 0 && latestBlock.index != 0){
            return getAdjustedDifficulty();
        }else {
            return latestBlock.difficulty;
        }
    }

    public static int getAdjustedDifficulty(){
        Block prevAdjustmentBlock = blockchain.get(blockchain.size() - 10);
        long timeExpected =  3/1000 * 10;
        long timeTaken = getPreviousBlock().timestamp - prevAdjustmentBlock.timestamp;
        if (timeTaken < timeExpected/2){
            return prevAdjustmentBlock.difficulty + 1;
        }else if (timeTaken > timeExpected * 2){
            return prevAdjustmentBlock.difficulty - 1;
        }else {
            return prevAdjustmentBlock.difficulty;
        }
    }

    //generate signature
    public static String signECDSA(PrivateKey privateKey, String message) {
        String result = "";
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes());

            byte[] sign = signature.sign();

            return Base64.getEncoder().encodeToString(sign);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //verify signature
    public static boolean verifyECDSA(PublicKey publicKey, String signed, String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes());

            //byte[] hex = Hex.decodeHex(signed);
            byte[] hex = Base64.getDecoder().decode(signed);
            boolean bool = signature.verify(hex);

            return bool;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static PublicKey getPublicKeyFromAddress(String address) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] de_key = Base64.getDecoder().decode(address);

        KeyFactory kf = KeyFactory.getInstance("EC");

        PublicKey pub = (ECPublicKey)kf.generatePublic(new X509EncodedKeySpec(de_key));

        return pub;
    }

//    public static boolean validUTXO(Block newBlock) throws NoSuchAlgorithmException, InvalidKeySpecException {
//        String txID = newBlock.getData().get(1).getTxIn().getTxOutId();
//        String signature = newBlock.getData().get(1).getTxIn().getSignature();
//        for(Block blk: BlockChain.blockchain){
//            if(blk.getData().get(0).getId().equals(txID)){
//                return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(0).getTxOut().getAddress()), signature, "owner");
//            }
////            else if (blk.getData().get(1).getId().equals(txID)){
////                return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(1).getTxOut().getAddress()), signature, "owner");
////            }
//            else if (blk.getData().size()>1){
//                if (blk.getData().get(1).getId().equals(txID)) {
//                    return verifyECDSA(getPublicKeyFromAddress(blk.getData().get(1).getTxOut().getAddress()), signature, "owner");
//                }
//            }
//        }
//        return false;
//    }




}
