package net.covers1624.wstool.wrapper.http;

import net.covers1624.quack.net.httpapi.EngineRequest;
import net.covers1624.quack.net.httpapi.HttpEngine;

/**
 * Created by covers1624 on 7/5/25.
 */
public final class Java8HttpEngine implements HttpEngine {

    @Override
    public EngineRequest newRequest() {
        return new Java8HttpEngineRequest();
    }
}
