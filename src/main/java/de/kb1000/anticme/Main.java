package de.kb1000.anticme;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

public class Main {
    public static void main(String[] args) throws IOException, IllegalClassFormatException {
        ClassFileTransformer classFileTransformer = new AntiCMEClassTransformer();
        try (
                final JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(Paths.get(args[0])));
                final JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(Paths.get(args[1])), Optional.ofNullable(jarInputStream.getManifest()).orElseGet(Manifest::new))
        ) {
            JarEntry jarEntry;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            CRC32 crc32 = new CRC32();
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (jarEntry.getName().endsWith(".class")) {
                    final ZipEntry zipEntry = new ZipEntry(jarEntry.getName());
                    nullsafe(jarEntry.getTime(), zipEntry::setTime);
                    nullsafe(jarEntry.getLastModifiedTime(), zipEntry::setLastModifiedTime);
                    nullsafe(jarEntry.getLastAccessTime(), zipEntry::setLastAccessTime);
                    nullsafe(jarEntry.getCreationTime(), zipEntry::setCreationTime);
                    nullsafe(jarEntry.getMethod(), zipEntry::setMethod);
                    zipEntry.setExtra(jarEntry.getExtra());
                    zipEntry.setComment(jarEntry.getComment());
                    byteArrayOutputStream.reset();
                    copy(jarInputStream, byteArrayOutputStream);
                    byte[] bytes = classFileTransformer.transform(null, jarEntry.getName().substring(0, jarEntry.getName().length() - 6), null, null, byteArrayOutputStream.toByteArray());
                    if (zipEntry.getMethod() == ZipEntry.STORED) {
                        zipEntry.setSize(bytes.length);
                        zipEntry.setCompressedSize(bytes.length);
                        crc32.reset();
                        crc32.update(bytes);
                        zipEntry.setCrc(crc32.getValue());
                    }
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(bytes);
                } else {
                    jarOutputStream.putNextEntry(jarEntry);
                    copy(jarInputStream, jarOutputStream);
                }
            }
        }
    }

    private static void copy(InputStream source, OutputStream sink)
            throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
    }

    private static <T> void nullsafe(T t, Consumer<T> consumer) {
        if (t != null) {
            consumer.accept(t);
        }
    }

    private static void nullsafe(int i, IntConsumer consumer) {
        if (i != -1) {
            consumer.accept(i);
        }
    }
}
