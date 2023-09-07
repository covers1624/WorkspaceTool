package net.covers1624.wt;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by covers1624 on 28/2/22.
 */
public class OfflineLaunch {

    public static void main(String[] argsArray) throws Throwable {
        LinkedList<String> args = new LinkedList<>();
        Collections.addAll(args, argsArray);
        args.add("--uuid");
        args.add("0");
        args.add("--accessToken");
        args.add("0");
        args.add("--userType");
        args.add("mojang");

        Method method = Class.forName("cpw.mods.bootstraplauncher.BootstrapLauncher").getMethod("main", String[].class);
        method.invoke(null, (Object) args.toArray(new String[0]));
    }
}
