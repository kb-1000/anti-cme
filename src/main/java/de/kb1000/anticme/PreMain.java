package de.kb1000.anticme;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;

public class PreMain {
    public static void premain(final String arg, final Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new AntiCMEClassTransformer(), true);
        instrumentation.retransformClasses(HashMap.class);
        instrumentation.retransformClasses(HashMap.class.getDeclaredClasses());
    }
}
