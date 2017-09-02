package org.corfudb.shell;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.slf4j.LoggerFactory;

/**
 * Created by mwei on 11/18/16.
 */
public class ShellMain {

    public static void main(String[] args) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("org.corfudb.shell"));
        IFn shell = Clojure.var("org.corfudb.shell", "-main");
        shell.invoke(args);
    }
}
