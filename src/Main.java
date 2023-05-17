import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Main {
    public static ArrayList<Transaction> storgedTransactions = new ArrayList<>();
    public static void main(String[]args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the port number: ");
        int portNo = scanner.nextInt();
        scanner.nextLine();
        Node node = new Node(portNo);
        while(true) {
            System.out.println("1. check block list");
            System.out.println("2. make transaction");
            System.out.println("3. check address");
            System.out.println("4. check UTXOs");
            System.out.println("5. mine block");
            String option = scanner.nextLine();
            if (Objects.equals(option, "1")) {
                if (BlockChain.blockchain.size() == 0) {
                    System.out.println("No block");
                } else {
                    for (Block blk : BlockChain.blockchain) {
                        System.out.println(blk + "\n");
                    }
                }

            } else if (Objects.equals(option, "2")) {
                Boolean added = false;
                System.out.println("Enter address: ");
                String address = scanner.nextLine();
                System.out.println("Enter the amount: ");
                double amount = scanner.nextDouble();
                scanner.nextLine();
                int listIndex = -1;
                for (int i = 0; i < BlockChain.UTXOs.size(); i++) {
                    if (BlockChain.UTXOs.get(i).getTxOut().amount >= amount) {
                        listIndex = i;
                        break;
                    }
                }

                if (listIndex != -1) {
                    String signature = BlockChain.signECDSA(node.getPriKey(), "owner");
                    // Verify the public of current node (transaction input node)
                    if (!BlockChain.verifyECDSA(node.getPubKey(), signature, "owner")) {
                        System.out.println("Verify transaction input signature is wrong");
                        return;
                    }
                    System.out.println("Verify transaction input signature success");
                    Transaction addedTransaction = new Transaction(new TxIn(BlockChain.UTXOs.get(listIndex).getId(), 0, signature), new TxOut(address, amount));
                    storgedTransactions.add(addedTransaction);
                    // Block bk1 = Miner.findBlock(BlockChain.getPreviousIndex() + 1, BlockChain.getPreviousHash(), new Date().getTime() / 1000, new Transaction(new TxIn(BlockChain.UTXOs.get(listIndex).getId(), 0, signature), new TxOut(address, amount)), BlockChain.getDifficulty(), Node.getAddress(),true);
//                    added = BlockChain.addBlock(bk1);
                    // No need to remove UTXO and send block first, we will check the remaining amount first.
                        for (int port : node.peerList) {
                            node.sendStagedTransactions(port, addedTransaction);
                        }

                    // If the UTXO amount > amount, we add one more block and transaction to send remaining amount to this address from this address
                    if (BlockChain.UTXOs.get(listIndex).getTxOut().amount > amount) {
                        double amonutDifferent = BlockChain.UTXOs.get(listIndex).getTxOut().amount - amount;
                        Transaction returnAmountTransaction = new Transaction(new TxIn(BlockChain.UTXOs.get(listIndex).getId(), 0, signature), new TxOut(node.getAddress(), amonutDifferent));
                        storgedTransactions.add(returnAmountTransaction);
//                        Block bkReturnAmount = Miner.findBlock(BlockChain.getPreviousIndex() + 1, BlockChain.getPreviousHash(), new Date().getTime() / 1000, new Transaction(new TxIn(BlockChain.UTXOs.get(listIndex).getId(), 0, signature), new TxOut(node.getAddress(), amonutDifferent)), BlockChain.getDifficulty(), Node.getAddress(),true);
//                        added = BlockChain.addBlock(bkReturnAmount);

                            for (int port : node.peerList) {
                                node.sendStagedTransactions(port, returnAmountTransaction);
                            }

                    }
                    BlockChain.UTXOs.remove(listIndex);


                } else {
                    System.out.println("no enough money\n");
                }

            } else if (Objects.equals(option, "3")) {
                System.out.println(node.getAddress() + "\n");

            } else if (Objects.equals(option, "4")) {
                if (BlockChain.UTXOs.size() == 0) {
                    System.out.println("no UTXO\n");
                } else {
                    for (int i = 0; i < BlockChain.UTXOs.size(); i++) {
                        System.out.println(i + 1 + ": " + BlockChain.UTXOs.get(i));
                    }
                    System.out.println();
                }
            } else if (Objects.equals(option, "5")) {
                Boolean added = false;
                if (storgedTransactions.isEmpty()) {
                    System.out.println("No staged transactions record");
                } else {
                    System.out.println("Now mine a new block to store the staged transactions");
                    Block bk1 = Miner.findBlock(BlockChain.getPreviousIndex() + 1, BlockChain.getPreviousHash(), new Date().getTime() / 1000, storgedTransactions, BlockChain.getDifficulty(), Node.getAddress(), true);
                    added = BlockChain.addBlock(bk1);
                    if (added) {
                            for (int port : node.peerList) {
                                node.sendBlock(port, bk1);
                            }
                        }
                    // Refresh storged transaction
                    storgedTransactions.clear();
                    }
                }
            }
        }
}

