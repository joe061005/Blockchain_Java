import java.io.Serializable;

public class Transaction implements Serializable {

    public String id;
    public TxIn txIn;
    public TxOut txOut;

    public Transaction(TxIn txIn, TxOut txOut){
        this.txIn = txIn;
        this.txOut = txOut;
        this.id = calcTxID();
    }

    public Transaction(String address, int index){
        this.txIn = null;
        this.txOut = new TxOut(address, 50.0);
        this.id = calcCoinbaseID(index);
    }

    public String calcTxID(){
        String txInContent = txIn.txOutId + Integer.toString(txIn.txOutIndex);
        String txOutContent = txOut.address + Double.toString(txOut.amount);
        return Block.sha256(txInContent + txOutContent);
    }

    public String calcCoinbaseID(int index){
        String txOutContent = txOut.address + Double.toString(txOut.amount);
        return Block.sha256(Integer.toString(index) + txOutContent);
    }

    public String toString(){
        return txIn != null? "TxID: " + id + ", " +  txIn + ", " + txOut : "TxID: " + id + ", "  + txOut;
    }

    public String getId() {
        return id;
    }

    public TxIn getTxIn() {
        return txIn;
    }

    public TxOut getTxOut() {
        return txOut;
    }
}
