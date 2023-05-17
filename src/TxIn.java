import java.io.Serializable;

public class TxIn implements Serializable {

    public String txOutId;
    public int txOutIndex;
    public String signature;

    public TxIn(String txOutId, int txOutIndex, String signature){
        this.txOutId = txOutId;
        this.txOutIndex = txOutIndex;
        this.signature = signature;
    }

    public String toString(){
        return "Input: Tx ID (" + txOutId +")  index (" + txOutIndex + ")  signature ("  + signature + ")";
    }

    public boolean verifySignature(){
          return true;
    }

    public int getTxOutIndex() {
        return txOutIndex;
    }

    public String getSignature() {
        return signature;
    }

    public String getTxOutId() {
        return txOutId;
    }
}
