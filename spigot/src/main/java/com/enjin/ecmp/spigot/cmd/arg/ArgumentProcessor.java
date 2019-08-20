package com.enjin.ecmp.spigot.cmd.arg;

import java.util.List;
import java.util.Optional;

public interface ArgumentProcessor<T> {

    List<String> tab();

    <A extends Object> Optional<T> parse(String arg);

}
