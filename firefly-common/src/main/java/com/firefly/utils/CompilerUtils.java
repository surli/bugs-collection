package com.firefly.utils;

import com.firefly.utils.exception.CommonRuntimeException;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompilerUtils {
    private static final Map<String, JavaFileObject> output = new ConcurrentHashMap<>();
    public static ClassLoader classLoader = new CompilerClassLoader(CompilerUtils.class.getClassLoader());
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    public static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public static Class<?> compileSource(String completeClassName, String source) throws Throwable {
        boolean result;
        try (JavaFileManager fileManager = CompilerUtils.getStringSourceJavaFileManager(compiler, null, null, Charset.forName("UTF-8"))) {
            CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Arrays.asList(new JavaSourceFromString(completeClassName, source)));
            result = task.call();
        }

        if (!result)
            return null;

        return getClassByName(completeClassName);
    }

    public static Class<?> getClassByName(String name) throws ClassNotFoundException {
        Class<?> ret = classCache.get(name);
        if (ret != null)
            return ret;

        synchronized (classCache) {
            ret = classCache.get(name);
            if (ret != null)
                return ret;

            ret = Class.forName(name, false, classLoader);
            classCache.put(name, ret);
            return ret;
        }
    }

    public static JavaFileManager getStringSourceJavaFileManager(JavaCompiler compiler, DiagnosticListener<? super JavaFileObject> diagnosticListener, Locale locale, Charset charset) {

        return new ForwardingJavaFileManager<StandardJavaFileManager>(compiler.getStandardFileManager(diagnosticListener, locale, charset)) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location,
                                                       String className, Kind kind, FileObject sibling)
                    throws IOException {
                JavaFileObject jfo = new ByteJavaObject(className, kind);
                output.put(className, jfo);
                return jfo;
            }
        };
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        /**
         * The source code of this "file".
         */
        final String code;

        /**
         * Constructs a new JavaSourceFromString.
         *
         * @param name the name of the compilation unit represented by this file
         *             object
         * @param code the source code for the compilation unit represented by
         *             this file object
         */
        public JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class ByteJavaObject extends SimpleJavaFileObject {

        private ByteArrayOutputStream baos;

        public ByteJavaObject(String name, Kind kind) {
            super(toURI(name), kind);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException, IllegalStateException,
                UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream openInputStream() throws IOException,
                IllegalStateException, UnsupportedOperationException {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        @Override
        public OutputStream openOutputStream() throws IOException,
                IllegalStateException, UnsupportedOperationException {
            return baos = new ByteArrayOutputStream();
        }
    }

    public static class CompilerClassLoader extends ClassLoader {

        public CompilerClassLoader(ClassLoader classLoader) {
            super(classLoader);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            JavaFileObject jfo = output.get(name);
            if (jfo != null) {
                byte[] bytes = ((ByteJavaObject) jfo).baos.toByteArray();
                output.remove(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }

    private static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new CommonRuntimeException(e);
        }
    }

}
