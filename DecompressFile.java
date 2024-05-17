import java.io.*;

/**
 * Module 15, Programming Project:
 * <br> 1. This class decompresses a file that was compressed using Huffman coding.
 * <br> 2. Taking a compressed file as input, then reconstructs the Huffman Tree from the file,
 * as it reads the encoded message, decodes it using the Huffman Tree, and writes the decoded 
 * message to a specified output file.
 * @author Shuyu Cai
 * @author Wilson Chung
 */

public class DecompressFile {

    /**
     * The main method where the execution of the program begins. It processes input arguments and handles file operations for decompression.
     *
     * @param args command line arguments, expecting two: the path to the compressed file and the path for the decompressed output file.
     * @author Stephen
     * @author Wilson
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java DecompressFile <compressed file> <decompressed file>");
            return;
        }

        String compressedFile = args[0];
        String decompressedFile = args[1];

        File compressed = new File(compressedFile);
        File decompressed = new File(decompressedFile);

        // Create the decompressed file if it doesn't exist
        if (!decompressed.exists()) {
            try {
                decompressed.createNewFile();
                System.out.println("Created new file: " + decompressedFile);
            } catch (IOException e) {
                System.err.println("Error creating new file: " + e.getMessage());
                return;
            }
        }

        try (BitInputStream bis = new BitInputStream(compressed);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            HuffmanTree hf = (HuffmanTree) ois.readObject();

            int messageLength = bis.readInt();

            String message = bis.readBits(messageLength);

            String decodedMessage = decodeBinaryString(message, hf);

            // Write the decoded message to the decompressed file
            try (FileWriter writer = new FileWriter(decompressed)) {
                writer.write(decodedMessage);
            } catch (IOException e) {
                System.err.println("Error writing to decompressed file: " + e.getMessage());
            }
            System.out.println("Decoded message written to " + decompressedFile);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("IO Exception: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        }
    }

    /**
     * Decodes a binary string using a given Huffman tree to reconstruct the original message.
     *
     * @param binaryString the string of binary data to decode.
     * @param huffmanTree the Huffman tree used for decoding.
     * @return the decoded string.
     * @author Wilson
     */
    public static String decodeBinaryString(String binaryString, HuffmanTree huffmanTree) {
        StringBuilder decodedString = new StringBuilder();
        HuffmanTree.HuffmanNode currentNode = huffmanTree.root;

        for (int i = 0; i < binaryString.length(); i++) {
            char bit = binaryString.charAt(i);
            currentNode = (bit == '0') ? currentNode.left : currentNode.right;

            if (currentNode.left == null && currentNode.right == null) {
                decodedString.append(currentNode.data);
                currentNode = huffmanTree.root; // Reset to root for next character
            }
        }

        return decodedString.toString();
    }
}

/**
 * A BitInputStream reads bits from a file. It extends FileInputStream to
 * provide methods for reading individual bits and other bit-level operations.
 * @author Wilson
 * @author Stephen
 */
class BitInputStream extends FileInputStream {
    private int currentByte; // the byte we're reading in
    private int numBits; // the number of bits that have been read in

    /**
     * Constructs a BitInputStream that reads from the specified file.
     *
     * @param file the file to read from
     * @throws FileNotFoundException if the file does not exist
     */
    public BitInputStream(File file) throws FileNotFoundException {
        super(file);
        currentByte = 0;
        numBits = 0;
    }

    /**
     * Reads the next bit from the input stream.
     *
     * @return the next bit (0 or 1), or -1 if the end of the file is reached
     * @throws IOException if an I/O error occurs
     */
    public int readBit() throws IOException {
        if (numBits == 0) { // no bits have been read yet
            // read one byte from FileInputStream
            int newByte = super.read();
            if (newByte == -1) return -1; // end of file
            // set the values of the current byte and the number of bits (8 when a byte is read in)
            currentByte = newByte;
            numBits = 8;
        }
        // shifts the digits to the right by numBits-1 so that the bit of interest is the rightmost
        // & 1 will turn every preceding bit to 0
        int bit = (currentByte >>> (numBits - 1)) & 1;
        numBits--;
        return bit;
    }

    /**
     * Reads the specified number of bits from the input stream.
     *
     * @param quantity the number of bits to read
     * @return a string representing the bits read
     * @throws IOException if an I/O error occurs
     */
    public String readBits(int quantity) throws IOException {
        StringBuilder bitString = new StringBuilder();
        for (int i = 0; i < quantity; i++) {
            int bit = readBit();
            if (bit == -1) break; // end of file
            bitString.append(bit);
        }
        return bitString.toString();
    }

    /**
     * Reads the next four bytes from the input stream and returns them as an integer.
     *
     * @return the next four bytes as an integer
     * @throws IOException if an I/O error occurs or if the end of the file is reached
     */
    public int readInt() throws IOException {
        int intValue = 0;
        for (int i = 0; i < 4; i++) {
            int oneByte = super.read();
            if (oneByte == -1) throw new EOFException("End of input reached");
            intValue = (intValue << 8) | (oneByte & 0xFF);
        }
        return intValue;
    }

    /**
     * Reads the next byte from the input stream.
     *
     * @return the next byte as an integer
     * @throws IOException if an I/O error occurs
     */
    public int readByte() throws IOException {
        return Integer.parseInt(readBits(8), 2);
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        super.close();
    }
}
