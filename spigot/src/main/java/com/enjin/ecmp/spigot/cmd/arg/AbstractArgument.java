package com.enjin.ecmp.spigot.cmd.arg;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractArgument<T> implements Argument<T> {

    private boolean required;

    public AbstractArgument(boolean required) {
        this.required = required;
    }

    public AbstractArgument() {
        this(true);
    }

    @Override
    public List<String> tab() {
        return new ArrayList<>();
    }

    @Override
    public boolean isRequired() {
        return required;
    }
}
