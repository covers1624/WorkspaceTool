package net.covers1624.wt.api.impl.script.runconfig;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.api.script.runconfig.RunConfigContainer;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 23/7/19.
 */
public class DefaultRunConfigContainer extends GroovyObjectSupport implements RunConfigContainer {

    private Map<String, RunConfig> runConfigs = new HashMap<>();

    @Override
    public Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (list.size() != 1) {
            throw new MissingMethodException(name, getClass(), list.toArray(), false);
        }
        Object arg0 = list.get(0);
        if (!(arg0 instanceof Closure)) {
            throw new MissingMethodException(name, getClass(), list.toArray(), false);
        }
        RunConfig runConfig = runConfigs.computeIfAbsent(name, e -> new DefaultRunConfig());
        Closure closure = (Closure) ((Closure) arg0).clone();//Eww
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.setDelegate(runConfig);
        if (closure.getMaximumNumberOfParameters() == 0) {
            closure.call();
        } else {
            closure.call(runConfig);
        }
        return runConfig;
    }

    @Override
    public Map<String, RunConfig> getRunConfigs() {
        return runConfigs;
    }
}
