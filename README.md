# Anti-CME

Anti-CME is a tool to help with debugging CMEs (`ConcurrentModificationException`) in Java.
It will insert a stacktrace of the modification that caused it as the cause of the CME, enabling you to debug the issue much easier.

Unfortunately, currently only `java.util.HashMap`, along with its derived collections and iterators (like keySet or values) are supported. This may change in future.

## Usage

If you're using DCEVM, which supports redefining classes with additional fields, then most likely all you need to do is add `-javaagent:<path/to/anti-cme.jar>` to your JVM arguments. If that didn't work, or you're not using DCEVM, use one of the following ways (depending on your java version):

### Java 8

Run the Anti-CME patcher as follows:
```bash
java -jar <path/to/anti-cme.jar> <path/to/java/lib/rt.jar> anti-cme-rt.jar
```

Now, use one of the following ways to "install" that patched JAR:

 * (not recommended) Replace the `rt.jar` of your Java installation with the patched `rt.jar`
 * (recommended) Add `-Xbootclasspath/p:anti-cme-rt.jar` to your JVM arguments.

### Java 9+

First, you'll have to extract the `classes` folder from `java.base.jmod`.
Since this special ZIP file has 4 magic number bytss prepended, not every archiver will accept it. Info-ZIP `unzip` (often present and used on Linux distributoons) should handle tnis fine.
If you're using 7-Zip, you may have to use Open As -> # to let it detect that there's a ZIP file 4 bytes after the file's start, and then open that ZIP.
Extract the contents of the `classes` folder, and repack them (not the `classes` folder itself, rather, the contents) as a ZIP file. I'll call it `java.base.zip` for this example.

Then, run the Anti-CME patcher on that JAR:
```bash
java -jar <path/to/anti-cme.jar> java.base.zip anti-cme-java.base.jar
```

Now, you can add `--patch-module=java.base=anti-cme-java.base.jar` to your JVM arguments to enable Anti-CME.

## How it works

The patcher as well as the Java agent inject an extra field into the target class, which then contains the stacktrace.
Whenever the collection's modification count field is written to, an exception is instantiated (but not thrown), and assigned to that field.
In addition to that, all places that throw a `ConcurrentModificationException` retrieve this field and add it as the cause through an additional constructor argument.

Due to instantiating an exception whenever structural modifications of any supported collection happen, this _will_ slow down your program, so please only use this for debugging purposes.
