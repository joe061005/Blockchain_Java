import java.io.Serializable;

public class TxOut implements Serializable {

    public String address;
    public Double amount;

    public TxOut(String address, Double amount){
        this.address = address;
        this.amount = amount;
    }

    public String toString(){
        return "Output: address (" + address +")  amount (" + amount + ")";
    }

    public String getAddress() {
        return address;
    }

    public Double getAmount() {
        return amount;
    }
}
