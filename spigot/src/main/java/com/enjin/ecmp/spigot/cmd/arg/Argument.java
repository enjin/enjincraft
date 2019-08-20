package com.enjin.ecmp.spigot.cmd.arg;

import com.enjin.ecmp.spigot.cmd.CommandContext;

import java.util.List;
import java.util.Optional;

public interface Argument<T> {

    List<String> tab();

    Optional<T> parse(CommandContext context, List<String> args);

    boolean isRequired();

}
