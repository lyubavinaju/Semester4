package ru.ifmo.rain.lyubavina.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation class for {@link JarImpler} interface
 *
 * @author elenalubavina
 */
public class Implementor implements JarImpler {
    /**
     * Recursively deletes a file or directory
     *
     * @param file {@link File} that will be deleted
     * @throws ImplerException when file cannot be deleted
     */
    private void recursiveDelete(File file) throws ImplerException {
        // до конца рекурсивного цикла
        if (!file.exists())
            return;

        //если это папка, то идем внутрь этой папки и вызываем рекурсивное удаление всего, что там есть
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    // рекурсивный вызов
                    recursiveDelete(f);
                }
            }
        }
        // вызываем метод delete() для удаления файлов и пустых(!) папок
        boolean deleted = file.delete();
        if (!deleted) {
            throw new ImplerException("cannot delete: " + file.getAbsolutePath());
        }
    }

    /**
     * Produces <var>.jar</var> file implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <var>Impl</var> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Incorrect arguments");
        }
        createDirectory(jarFile);
        Path directory;
        try {
            directory = Files.createTempDirectory("temp");
        } catch (IOException e) {
            throw new ImplerException("unable to create temp directory");
        }
        try {

            implement(token, directory);

            Path filePath = getFilePath(directory, token, "Impl.java");
            compile(filePath);

            Path classPath = getFilePath(directory, token, "Impl.class");

            String classLocalPath = getFilePath(Paths.get(""), token, "Impl.class").toString();

            Manifest manifest = createManifest();

            createJar(manifest, classPath, classLocalPath, jarFile);

        } finally {
            try {
                recursiveDelete(directory.toFile());
            } catch (ImplerException e) {
                System.err.println(e.getMessage());
            }
        }

    }

    /**
     * Represents {@link Method} correctly
     */
    private class CustomMethod {
        /**
         * Instance of {@link Method}
         */
        private Method m;

        /**
         * Constructs a representation for given {@link Method}
         *
         * @param m given {@link Method}
         */
        CustomMethod(Method m) {
            this.m = m;
        }

        /**
         * Checks if given obj and this are equal.
         * Two {@link CustomMethod} are equal if their names, parameter  types and  return types are equal.
         *
         * @param obj object for comparison with this
         * @return true if this equals obj
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CustomMethod) {
                CustomMethod o2 = (CustomMethod) obj;
                return this.m.getName().equals(o2.m.getName()) &&
                        Arrays.equals(this.m.getParameterTypes(), o2.m.getParameterTypes())
                        && this.m.getReturnType().equals(o2.m.getReturnType());
            } else {
                return false;
            }
        }

        /**
         * Base for hashCode
         */
        private final static int X = 41;
        /**
         * Mod for hashCode
         */
        private final static int Q = (1 << 31) - 1;

        /**
         * Calculates hashCode for this using a polynomial hash
         *
         * @return hashCode of this
         */
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += this.m.getName().hashCode() * X * X;
            hashCode %= Q;
            hashCode += Arrays.hashCode(this.m.getParameterTypes()) * X;
            hashCode %= Q;
            hashCode += this.m.getReturnType().hashCode();
            hashCode %= Q;
            return hashCode;
        }


    }


    /**
     * Produces code implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <var>Impl</var> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <var>root</var> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <var>$root/java/util/ListImpl.java</var>
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be
     *                         generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (token == null || root == null || token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Incorrect arguments");
        }
        /*bug fixed*/
        if (Modifier.isPrivate(token.getModifiers()))
            throw new ImplerException();


        Path filePath = getFilePath(root, token, "Impl.java");

        createDirectory(filePath);
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(filePath))) {
            StringBuilder stringBuilder = new StringBuilder();
            makePackage(token, stringBuilder);
            stringBuilder.append(System.lineSeparator());
            //annotations
            stringBuilder.append("@SuppressWarnings(\"unchecked\")").append(System.lineSeparator())
                    .append("@Deprecated").append(System.lineSeparator());
            //class head
            stringBuilder.append("public class ").append(token.getSimpleName()).append("Impl ");
            if (token.isInterface()) {
                stringBuilder.append("implements ");
            } else stringBuilder.append("extends ");

            /*bug fixed */
            stringBuilder.append(token.getCanonicalName());
            stringBuilder.append(" { ");
            printWriter.println(stringBuilder.toString());


            if (!token.isInterface()) {
                implementConstructors(printWriter, token);
            }
            implementAbstractMethods(printWriter, token);
            printWriter.println(" } ");

        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
//        Scanner scanner = null;
//        try {
//            scanner = new Scanner(filePath);
//            while (scanner.hasNextLine()) {
//                System.out.print(scanner.nextLine());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    /**
     * Filter given methods and returns only abstract
     *
     * @param declaredMethods given methods
     * @return {@link List} of {@link CustomMethod}
     */
    private List<CustomMethod> filterMethods(Method[] declaredMethods) {

        return Arrays.stream(declaredMethods)
                .filter(method -> Modifier.isAbstract(method.getModifiers())).map(CustomMethod::new).collect(Collectors.toList());
    }

    /**
     * Creates a string representation of a given {@link CustomMethod}
     *
     * @param m given {@link CustomMethod}
     * @return {@link String} that represents given {@link CustomMethod}
     */
    private String methodToString(CustomMethod m) {
        StringBuilder method = new StringBuilder();
        int modifiers = m.m.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT;
        method.append(Modifier.toString(modifiers)).append(" ")

                .append(m.m.getReturnType().getCanonicalName()).append(" ")

                .append(m.m.getName()).append(getParameters(m.m)).append(getExceptions(m.m))
                .append(" {").append(System.lineSeparator()).append("return ").append(getDefaultVal(m.m.getReturnType()))
                .append(";").append("}").append(System.lineSeparator());

        return method.toString();
    }

    /**
     * Returns string representation of default value for given class
     *
     * @param m given {@link Class}
     * @return {@link String} that represents default value for given class
     */
    private String getDefaultVal(Class<?> m) {
        if (boolean.class.equals(m)) return "false";
        if (void.class.equals(m)) return "";
        if (m.isPrimitive()) return "0";

        return null;
    }

    /**
     * Returns string representation of given {@link Constructor}
     *
     * @param c     given {@link Constructor}
     * @param token type token of {@link Class} of given constructor
     * @return {@link String} that represents given constructor
     */
    private String constructorToString(Constructor c, Class<?> token) {
        StringBuilder constructor = new StringBuilder();
        int modifiers = c.getModifiers() & ~Modifier.TRANSIENT;
        constructor.append(Modifier.toString(modifiers)).append(" ")
                .append(token.getSimpleName()).append("Impl").append(getParameters(c)).append(getExceptions(c))
                .append(" {").append(System.lineSeparator()).append("super").append(getArguments(c))
                .append(";").append("}").append(System.lineSeparator());


        return constructor.toString();
    }

    /**
     * Returns string representation of exceptions of given {@link Executable}
     *
     * @param c given {@link Executable}
     * @return {@link String} that represents exceptions({@link Exception})
     */
    private String getExceptions(Executable c) {

        Class[] classes = c.getExceptionTypes();
        if (classes.length == 0) return "";
        return " throws " + Arrays.stream(classes).map(Class::getCanonicalName).collect(Collectors.joining(", "));
    }

    /**
     * Returns string representation of parameters of given {@link Executable}
     *
     * @param c given {@link Executable}
     * @return {@link String} that represents parameters
     */
    private String getParameters(Executable c) {
        return "(" + Arrays.stream(c.getParameters())
                .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ")) + ")";
    }

    /**
     * Returns string representation of arguments of given {@link Executable}
     *
     * @param c given {@link Executable}
     * @return {@link String} that represents arguments
     */
    private String getArguments(Executable c) {
        return "(" + Arrays.stream(c.getParameters())
                .map(Parameter::getName)
                .collect(Collectors.joining(", ")) + ")";
    }


    /**
     * Creates parent directory for file
     *
     * @param filePath path to file to create parent directory
     * @throws ImplerException if directory cannot be created
     */
    private void createDirectory(Path filePath) throws ImplerException {
        if (filePath.getParent() != null) {
            try {
                Files.createDirectories(filePath.getParent()); // an exception is not thrown if the directory could not be created because it already exists.
            } catch (IOException e) {
                throw new ImplerException(e.getMessage());
            }
        }
    }

    /**
     * Resolves path to file
     *
     * @param directory path to parent directory
     * @param token     type token of class to resolve path
     * @param ending    ending of path
     * @return {@link Path} to file
     */
    private Path getFilePath(Path directory, Class<?> token, String ending) {
        return directory
                .resolve(token.getPackageName()
                        .replace('.', File.separatorChar))
                .resolve(token.getSimpleName() + ending);

    }

    /**
     * Implements constructors from {@link Class}
     *
     * @param printWriter {@link PrintWriter} for writing string representation of constructors to file
     * @param token       type token of class
     * @throws ImplerException if there are no public constructors in class
     */
    private void implementConstructors(PrintWriter printWriter, Class<?> token) throws ImplerException {
        List<Constructor> list = Arrays.stream(token.getDeclaredConstructors())
                .filter(t -> !Modifier.isPrivate(t.getModifiers()))
                .collect(Collectors.toList());
        if (list.size() == 0) throw new ImplerException("no public constructor in class");
        for (Constructor c : list
        ) {

            printWriter.println(constructorToString(c, token));
        }
    }

    /**
     * Implements abstract methods from {@link Class}
     *
     * @param printWriter {@link java.io.PrintWriter} for writing string representation of methods to file
     * @param token       type token of class
     */
    private void implementAbstractMethods(PrintWriter printWriter, Class<?> token) {
        HashSet<CustomMethod> methods = new HashSet<>(filterMethods(token.getMethods()));
        Class<?> token1 = token;
        while (token1 != null) {
            methods.addAll(filterMethods(token1.getDeclaredMethods()));
            token1 = token1.getSuperclass();
        }

        for (CustomMethod method : methods
        ) {
            printWriter.println(methodToString(method));
        }
    }

    /**
     * Creates {@link Manifest} with main class
     *
     * @return created {@link Manifest}
     */
    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    /**
     * Compiles file
     *
     * @param filePath {@link Path} to compiling class
     * @throws ImplerException if class is not compiled
     */
    private void compile(Path filePath) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new ImplerException();
        String[] args = new String[]{
                filePath.toString()
        };

        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) throw new ImplerException();
    }

    /**
     * Creates Jar file
     *
     * @param manifest       {@link Manifest} for jar file
     * @param classPath      {@link Path} to main class
     * @param classLocalPath local path to main class
     * @param jarFile        {@link Path} path for creating jar file
     * @throws ImplerException if jar file is not created
     */
    private void createJar(Manifest manifest, Path classPath, String classLocalPath, Path jarFile) throws ImplerException {
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry(classLocalPath));
            Files.copy(classPath, writer);
        } catch (IOException e) {
            throw new ImplerException("cannot create jar");
        }
    }

    /**
     * Appends package string representation to {@link StringBuilder}
     *
     * @param token         type token of class
     * @param stringBuilder {@link StringBuilder} that represents storage
     */
    private void makePackage(Class<?> token, StringBuilder stringBuilder) {
        if (token.getPackageName().length() > 0) {
            stringBuilder.append("package ").append(token.getPackageName()).append(";");
        }
    }


    /**
     * Main method of class:
     * implements class if two arguments are given,
     * creates Jar file if three arguments are given.
     * Two args: className root. Three args: -jar className root
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("2 or 3 arguments expected. Try again");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Null argument not allowed here. Try again");
            }
        }
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

}