package ru.ifmo.rain.lyubavina.walk;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.stream.Stream;

public class RecursiveWalk {
    private static final int FNV_32_PRIME = 0x01000193;
    private static final String invalidFileHash = "00000000";

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.out.println("invalid arguments. please use 'java Walk <входной файл> <выходной файл>'");
            return;
        }

        //Check input and output file
        try {
            Path inputFilePath = Paths.get(args[0]);
            if (!Files.exists(inputFilePath) || !Files.isReadable(inputFilePath)) {
                System.out.println("cannot read input file");
                return;
            }

            Path outputFilePath = Paths.get(args[1]);
            if (!Files.exists(outputFilePath)) {
                if (!Files.exists(outputFilePath.getParent())) {
                    try {
                        Files.createDirectory(outputFilePath.getParent());
                    } catch (Exception e) {
                        System.out.println("cannot create directory");
                        return;
                    }
                }
            } else if (!Files.isWritable(outputFilePath)) {
                System.out.println("output file is not writable sorry");
                return;
            }

        } catch (InvalidPathException | NullPointerException e) {
            System.out.println("invalid input or output file path, try again");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        //calculate hashes
        try (BufferedReader input = new BufferedReader(new FileReader(new File(args[0])))) {
            String name;

            ArrayDeque<String> strings = new ArrayDeque<>();
            while ((name = input.readLine()) != null) {

                try {
                    Path path = Paths.get(name);
                    if (!Files.isDirectory(path)) {
                        strings.addLast(FNV(name) + " " + name);

                    } else {
                        try (Stream<Path> files = Files.walk(path)) {
                            files.filter(file -> Files.isRegularFile(file)).forEach(file -> strings.addLast(FNV(file.toString()) + " " + file.toString()));
                        } catch (IOException e) {
                            System.out.println("cannot read files " + path);
                            return;
                        }
                    }
                } catch (Exception e) {
                    strings.addLast(invalidFileHash + " " + name);
                }

            }
            input.close();

            try (PrintWriter printWriter = new PrintWriter(new File(args[1]))) {

                for (String string : strings
                ) {
                    printWriter.write(string + "\n");
                }
            } catch (Exception e) {
                System.out.println("output writing error");
            }
        } catch (Exception e) {
            System.out.println("reading files error");
        }


    }

    private static String FNV(String fileName) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(fileName)))) {
            int hval = 0x811c9dc5;

            int c;
            while ((c = bufferedInputStream.read()) != -1) {
                hval *= FNV_32_PRIME;
                hval ^= c;
            }
            return String.format("%08x", hval);

        } catch (Exception e) {
            return invalidFileHash;
        }
    }
}