package com.enjin.ecmp.spigot.cmd.arg;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractArgumentProcessor<T> implements ArgumentProcessor<T> {

    @Override
    public List<String> tab() {
        return new ArrayList<>();
    }

}
