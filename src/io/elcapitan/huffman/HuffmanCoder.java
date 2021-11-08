package io.elcapitan.huffman;

import io.elcapitan.huffman.io.BitWriter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class HuffmanCoder {
    private String message;
    private String code;
    private HuffmanNode root;
    private Map<Character, String> codeDict;
    private Map<Character, Double> frequencies;

    public HuffmanCoder() {
        this("");
    }

    public HuffmanCoder(String message) {
        setMessage(message);
    }

    public HuffmanCoder(File file) throws IOException {
        setFile(file);
    }

    public void setFile(File file) throws IOException {
        setMessage(readFile(file));
    }

    public void saveToFile(File file) throws IOException {
        BitWriter writer = new BitWriter(file);
        writeTree(writer);
        writeDiscardBits(writer);
        writeMessage(writer);
        writer.close();
    }

    public double getFrequency(char c) {
        return frequencies.getOrDefault(c, 0d);
    }

    public String getCode(char c) {
        return codeDict.get(c);
    }

    public Map<Character, String> getCodeDict() {
        return codeDict;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        generate();
    }

    public String getEncoded() {
        return code;
    }

    private void writeTree(BitWriter writer) throws IOException {
        writeNode(root, writer);
    }

    private void writeNode(HuffmanNode n, BitWriter writer) throws IOException {
        if (n == null) return;
        if (n.isLeaf()) {
            writer.writeBit(true);
            writer.writeByte((byte) n.getC());
        } else {
            writer.writeBit(false);
            writeNode(n.getLeft(), writer);
            writeNode(n.getRight(), writer);
        }
    }

    private void writeDiscardBits(BitWriter writer) throws IOException {
        writer.writeBits(code.length() % 8, 3);
    }

    private void writeMessage(BitWriter writer) throws IOException {
        for (char b : code.toCharArray()) {
            writer.writeBit(b == '1');
        }
    }

    private void generate() {
        generateFrequencies();
        generateTree();
        generateCodeDict();
        encode();
    }

    private void generateFrequencies() {
        frequencies = new HashMap<>();
        char[] unique = message.chars().distinct()
                .mapToObj(c -> String.valueOf((char) c)).collect(Collectors.joining()).toCharArray();

        for (char letter : unique) {
            double f = calculateFrequency(letter);
            frequencies.put(letter, f);
        }
    }

    private double calculateFrequency(char letter) {
        int count = 0;
        for (char c : message.toCharArray()) {
            if (letter == c) count++;
        }
        return (double) count / message.length();
    }

    private void generateTree() {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (char letter : frequencies.keySet()) {
            pq.add(new HuffmanNode(getFrequency(letter), letter));
        }

        while (pq.size() > 1) {
            pq.add(new HuffmanNode(pq.poll(), pq.poll()));
        }
        root = pq.poll();
    }

    private void generateCodeDict() {
        codeDict = new HashMap<>();
        if (root.isLeaf()) codeDict.put(root.getC(), "0");
        else generateCodes(root, "");
    }

    private void generateCodes(HuffmanNode n, String codePart) {
        if (n == null) return;
        if (n.isLeaf()) {
            codeDict.put(n.getC(), codePart);
            return;
        }
        generateCodes(n.getLeft(), codePart + "0");
        generateCodes(n.getRight(), codePart + "1");
    }

    private void encode() {
        StringBuilder builder = new StringBuilder();
        for (char c : message.toCharArray()) {
            builder.append(codeDict.get(c));
        }
        code = builder.toString();
    }

    private String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        int c = reader.read(buffer);
        while (c > 0) {
            builder.append(buffer, 0, c);
            c = reader.read(buffer);
        }
        reader.close();
        return builder.toString();
    }
}
