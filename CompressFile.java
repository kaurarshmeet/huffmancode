import java.io.*;
import java.util.Scanner;
import java.util.InputMismatchException;

/**
 * Module 15, Programming Project:
 * <br> 1. This class compresses a text file using Huffman coding.
 * <br> 2. It takes an input file, builds a Huffman Tree based on the contents of the text,
 * and writes the Huffman Tree and encoded output message to a specified target file.
 * @author Arshmeet Kaur
 * @author Vincent Tran
 */
public class CompressFile {

    /**
     * This main method ensures that the program works from the commmand line.
     * @author Vincent Tran
     * @param args user should provide the complete path to an input file and output file
     *             seperated with a space.*/
    public static void main(String[] args) {
        if(args.length == 2) {
            File inputFile = new File(args[0]);
            File outputFile = new File(args[1]);

            try {
                System.out.println("Compressing file.");
                compressFile(inputFile, outputFile);
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }
        else {
            System.out.println("Usage: java CompressFile.java (InputFile) (OutputFile)");
        }
    }

    /**
     * This is the method which calls to all other methods to read the file, calculate frequencies
     * of characters within the file, construct a huffman tree, get the codes that correspond to each character,
     * construct the message in the Huffman encoding and output the tree and message to the target file
     * in that order.
     * @param source the source file the user wishes to compress.
     * @param target the intended location of the compressed contents.
     * @throws IOException to handle any missing files being put at source/target.
     * @throws ClassNotFoundException to deal with typecasting errors.
     * @author Vincent Tran
     * @author Arshmeet Kaur
     */
    public static void compressFile(File source, File target) throws IOException, ClassNotFoundException {

        /* Author: Vincent
        readFile(): returns a String containing the full file's text.
        */
        String messageInput = readFile(source);

        /* Author: Vincent
        calculateFrequencies(): takes messageInput (full file's text in a string)
        returns an array of the frequencies of each ASCII character in the messageInput.
        */
        int[] frequencies = calculateFrequencies(messageInput); // similar to my getCharacterFrequency method

        /* Author: Arshmeet
        getHuffmanTree(): takes the array of frequenices of each character.
        returns a huffman tree object.
        */
        HuffmanTree hf = getHuffmanTree(frequencies);

        /* Author: Arshmeet
        getCode(): takes the huffman tree object's root.
        returns a key of the huffman codes for each ASCII character in the original file.
         */
        String[] charKey = hf.getCode(hf.root); // calls assignCode

        /* NOTE: Delete when done. */
        System.out.printf("%-15s%-15s%-15s%-15s\n", "ASCII Code", "Character", "Frequency", "Encoding");
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] != 0) {
                if (i == 10) { // ASCII for 10 is new line
                    System.out.printf("%-15d%-15s%-15d%-15s\n", i, "New Line", frequencies[i], charKey[i]);
                } else if (i == 13) { // ASCII for 13 is Carriage Return (Ignore when decompressing)
                    System.out.printf("%-15d%-15s%-15d%-15s\n", i, "CR", frequencies[i], charKey[i]);
                } else {
                    System.out.printf("%-15d%-15s%-15d%-15s\n", i, (char) i + "", frequencies[i], charKey[i]);
                }
            }
        }

        /* Author: Vincent
        writeMessage() takes the array of codes for each character and the input message
        returns the encoded method that we want to put into the output file. */
        String outputMessage = writeMessage(charKey, messageInput);

        /* Author: Arshmeet
        - constructs a BitOutputStream object (which extends FileOutputStream)
        - - creates in append mode.
        - constructs an ObjectOutputStream wrapping around the BitOutputStream
        - writes the Huffman Tree Object, length of the message outputted and message outputted
        in that order.
        */
        BitOutputStream bos = new BitOutputStream(target, true);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(hf);
        bos.writeInt(outputMessage.length());
        bos.writeBits(outputMessage);
        oos.flush();
        bos.flush();
        oos.close();
    }

    public static String decodeBinaryString(String binaryString, HuffmanTree huffmanTree) {
        StringBuilder decodedString = new StringBuilder();
        HuffmanTree.HuffmanNode currentNode = huffmanTree.root;


        for (int i = 0; i < binaryString.length(); i++) {
            char bit = binaryString.charAt(i);
            if (bit == '0') {
                currentNode = currentNode.left;
            } else if (bit == '1') {
                currentNode = currentNode.right;
            }


            if (currentNode.left == null && currentNode.right == null) {
                // Reached a leaf node, append the character and reset to root
                decodedString.append(currentNode.data);
                currentNode = huffmanTree.root;
            }
        }


        return decodedString.toString();
    }

    /**
     * Takes input file and returns a string containing entire text from the file.
     * Utilizes the scanner class.
     * @param source File to read text from
     * @return String containing text from file
     * @throws FileNotFoundException
     * @author Vincent Tran
     */
    public static String readFile(File source) throws FileNotFoundException {
        Scanner input = new Scanner(source);
        String content = input.useDelimiter("\\Z").next(); // delimiter allows entire text to be read instead of single word using .next()
        input.close();
        return content;
    }

    /**
     * Takes a string and casts each character as an integer to convert it into ASCII code, increasing
     * the count by 1 using the ASCII code of the character as the index
     * @param text String to count frequency of characters for
     * @return Integer array containing counts of each character
     * @author Vincent Tran
     */
    public static int[] calculateFrequencies(String text) {
        int[] frequencies = new int[256];

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);
            int index = (int) c;

            if (index > 256){
                continue;
            } else {
                frequencies[index]++; // count the character in text
            }
        }
        System.out.println("exiting");
        return frequencies;
    }

    /**
     * Constructs a Huffman Tree using a heap.
     * <br> 1. Creates a forest of Huffman Trees with their weights (frequencies) and their data (characters).
     * <br> 2. Inserts the forest of trees into a min heap.
     * <br> 3. Removes the two lowest-weighted trees, combines them, inserts the new tree.
     * <br> 4. At the end, removes the final Huffman Tree from the heap.
     * @param frequencies the array of frequencies for each character. In conjunction with the character itself,
     *                    this information is used to create leaf nodes to insert into the heap.
     * @return The Huffman Tree Object
     * @author Arshmeet Kaur
     */
    public static HuffmanTree getHuffmanTree(int[] frequencies) {

        Heap<HuffmanTree> minheap = new Heap<>();

        int i = 0;
        while (i < frequencies.length) {
            if (frequencies[i] > 0)
                minheap.add(new HuffmanTree(frequencies[i], (char)i));
            i++;
        }

        /* Special Case: there's only one character in the file. */
        if (minheap.getSize() == 1){
            /* construct a new tree with the actual Tree in the heap a new character with no weight
            this way there will be a node to traverse through so we can get a code */
            minheap.add(new HuffmanTree(minheap.remove(), new HuffmanTree(0, ' ')));
        }

        /* Regular case: follow the huffman coding algorithm.
        Combine the two lowest-weight trees (.remove() from a minheap) into one node
        and reinsert into the minheap.
        At the end, the top of the heap is the final tree with the highest frequencies at the top.
         */
        while (minheap.getSize() > 1) {
            HuffmanTree h1 = minheap.remove(); // Remove the smallest weight trees
            HuffmanTree h2 = minheap.remove(); // Remove the next smallest weight
            minheap.add(new HuffmanTree(h1, h2)); // Combine two trees
        }
        return minheap.remove();
    }

    /**
     * Uses the key which maps codes to each character to convert each character
     * in the given string to its huffman code.
     * <br> appends each encoded character to a stringbuilder.
     * <br> once all characters are converted, strinbuilder converted to string and returned.
     *
     * @param charKey the array of codes for each character
     * @param messageInput the ASCII string that we want to convert to a huffman code string
     * @return the message (a string of 1's and 0s) to write to the target file.
     * @author Vincent Tran
     */
    public static String writeMessage(String[] charKey, String messageInput) {

        StringBuilder message = new StringBuilder(); // empty string builder

        for(int i = 0; i < messageInput.length(); i++) {
            char currentCharacter = messageInput.charAt(i);
            int asciiCode = currentCharacter;

            if (asciiCode > 256) {
                continue;
            } else {
                message.append(charKey[asciiCode]);
            }
        }

        return message.toString();
    }
}

/**
 * Generic implementation of a Heap using an array list.
 * @author Vincent Tran
 * @param <E> This will be used for the tree, to create a Heap of type HuffmanTree
 */
class Heap<E extends Comparable<E>> implements Serializable {
    private java.util.ArrayList<E> heap = new java.util.ArrayList<E>();

    /** default heap constructor */
    public Heap() {
    }

    /** create heap from an array of objects */
    public Heap(E[] objects) {
        for (int i = 0; i < objects.length; i++)
            add(objects[i]);
    }

    /** add object to heap */
    public void add(E object) {
        heap.add(object); // add to the heap
        int currentIndex = heap.size() - 1; // index of the last node

        while (currentIndex > 0) {
            int parentIndex = (currentIndex - 1) / 2;
            if (heap.get(currentIndex).compareTo(heap.get(parentIndex)) > 0) { // if the current object is greater than its parent
                // swap objects
                E temp = heap.get(currentIndex);
                heap.set(currentIndex, heap.get(parentIndex));
                heap.set(parentIndex, temp);
            }
            else
                break; // tree is a heap

            currentIndex = parentIndex;
        }
    }

    /** remove root from the heap and return the removed object */
    public E remove() {
        if (heap.size() == 0)
            return null; // heap is empty

        E removedObject = heap.get(0);

        heap.set(0, heap.get(heap.size() - 1));
        heap.remove(heap.size() - 1);

        int currentIndex = 0;
        while (currentIndex < heap.size()) {
            int leftChildIndex = 2 * currentIndex + 1;
            int rightChildIndex = 2 * currentIndex + 2;

            // find maximum between two children
            if (leftChildIndex >= heap.size())
                break; // tree is a heap

            int maxIndex = leftChildIndex; // set max to left child
            if (rightChildIndex < heap.size()) {
                if (heap.get(maxIndex).compareTo(heap.get(rightChildIndex)) < 0) { // if left child is less than the right
                    maxIndex = rightChildIndex; // set max to right child
                }
            }

            if (heap.get(currentIndex).compareTo(heap.get(maxIndex)) < 0) { // if current node is less than the max
                // swap objects
                E temp = heap.get(maxIndex);
                heap.set(maxIndex, heap.get(currentIndex));
                heap.set(currentIndex, temp);
                currentIndex = maxIndex;
            }
            else
                break; // tree is a heap
        }

        return removedObject;
    }

    /** return number of nodes in the tree */
    public int getSize() {
        return heap.size();
    }
}

/**
 *
 * This class creates a Huffman Tree for encoding/decoding characters.
 * <br> It contains an inner class HuffmanNode to represent each "node" or character in the tree.
 *
 * <p>
 *     This class contains methods that allow users to compare Huffman trees based on weight,
 *     and get the array mapping characters to Huffman codes.
 * </p>
 *
 * @author Arshmeet Kaur
 */
class HuffmanTree implements Comparable<HuffmanTree>, Serializable {

    // for easy serialization.
    private static final long serialVersionUID = 2939177167726658626L;

    HuffmanNode root;
    private String[] encodings;

    /**
     * Constructs a huffman tree with two children. Sets left and right child and weight
     * which is the combined weight of the child trees.
     * @param h1 left child.
     * @param h2 right child.
     */
    public HuffmanTree(HuffmanTree h1, HuffmanTree h2) {
        root = new HuffmanNode();
        root.left = h1.root;
        root.right = h2.root;
        root.weight = h1.root.weight + h2.root.weight;
    }

    /**
     * Constructs a huffman tree which is a leaf. Sets it weight and the "data", which is
     * the character that it holds.
     * @param weight
     * @param data
     */
    public HuffmanTree(int weight, char data) {
        this.root = new HuffmanNode(weight, data);
    }

    /**
     * compareTo() method is implemented to be in reverse order. In the Huffman Algorithm, we use a MinHeap.
     * However, here, we have used a max heap, so to easily convert to a min heap, we reverse the comparing order.
     * @param h the object to be compared.
     * @return -1 if the current weight is larger, 0 if weight is equal, 1 if current weight is lesser.
     */
    @Override
    public int compareTo(HuffmanTree h) {
        boolean hDifference = this.root.weight > h.root.weight;
        if (hDifference)
            return -1;
        else if (this.root.weight == h.root.weight)
            return 0;
        else
            return 1;
    }

    /**
     * Returns the key of ASCII characters to Huffman encoding string.
     * @param root is the "root" of the HuffmanNode passed into the HuffmanTree.
     * @return an array mapping the ASCII character (index of array) to the string of its huffman code.
     */
    public String[] getCode(HuffmanTree.HuffmanNode root) {
        // if the tree is empty...
        if (root == null)
            return null;
        // the string array of codes is 256, one for each ASCII character
        String[] encoding = new String[256];
        // fill the array with the code for each character
        assignCode(root, encoding);
        this.encodings = encoding;
        return encoding;
    }

    /**
     * Helper method of getCode() fills the encoding array passed in.
     * @param root is the rood of the node
     * @param encoding is the empty array which maps ASCII character to its huffman code.
     */
    public static void assignCode(HuffmanTree.HuffmanNode root, String[] encoding) {
        if (root.left != null) {
            // add zeros going leftwards
            root.left.code = root.code + "0";
            assignCode(root.left, encoding);

            // add ones going rightwards
            root.right.code = root.code + "1";
            assignCode(root.right, encoding);
        } else {
            // when you hit the leaf (containing characters), save the code that has been built
            // in the recursive calls
            encoding[(int) root.data] = root.code;
        }
    }

    /**
     * Need Inner Class for the HuffmanNode class
     * Each HuffmanNode has a character, it's weight (frequency),
     * a left and right child, and a string code which represents its Huffman code.
     * @author Arshmeet Kaur */
    class HuffmanNode implements Serializable {
        /**
         * data holds the character at each node.
         * weight holds the weight of the node.
         * left and right and the children of the node
         * code is path of 0s or 1s that lead to the node.
         */
        char data;
        int weight;
        HuffmanNode left; HuffmanNode right;
        String code = "";

        /** default constructor. */
        public HuffmanNode(){}

        /** value constructor gives each node character and weight */
        public HuffmanNode(int weight, char data) {
            this.weight = weight;
            this.data = data;
        }
    }
}

class BitOutputStream extends FileOutputStream implements Serializable {

    private int currentByte = 0;
    private int numBits = 0;
    boolean append;

    public BitOutputStream(File f, boolean append) throws IOException {
        super(f, append);
        this.append = append;
    }

    public void writeInt(int count) throws IOException {
        // extract each byte of the integer and write one by one
        super.write((count >> 24) & 0xFF); //shift first byte to lsb
        super.write((count >> 16) & 0xFF); // shift second byte to lsb
        super.write((count >> 8) & 0xFF); // shift third byte to lsb
        super.write(count & 0xFF); // shift fourth byte to lsb
    }

    public void writeBits(String bitString) throws IOException {
        for (char b : bitString.toCharArray()) {

            if (b == '0' || b == '1') {
                numBits++;
                int currentBit =  ( ((int)b)- 48);
                currentByte = (currentByte << 1) | currentBit;
                if (numBits == 8) {
                    super.write(currentByte);
                    numBits = 0;
                    currentByte = 0;
                }
            } else {
                throw new InputMismatchException("Input can only be '0' or '1'");
            }

        }
    }

    public void close() throws IOException {
        if (numBits > 0) {
            currentByte <<= (8 - numBits);
            super.write(currentByte);
        }
        super.close();
    }
}
