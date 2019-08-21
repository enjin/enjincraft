package com.enjin.ecmp.spigot.cmd.arg;

import com.enjin.ecmp.spigot.i18n.Locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocaleArgumentProcessor extends AbstractArgumentProcessor<Locale> {

    public static final LocaleArgumentProcessor INSTANCE = new LocaleArgumentProcessor();

    @Override
    public List<String> tab() {
        List<String> result = new ArrayList<>();

        result.addAll(Arrays.asList(Locale.values()).stream()
                .map(Locale::language)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public <A> Optional<Locale> parse(String arg) {
        Optional result = Optional.empty();

        for (Locale locale : Locale.values()) {
            if (locale.locale().equalsIgnoreCase(arg) || locale.language().equalsIgnoreCase(arg)) {
                result = Optional.of(locale);
                break;
            }
        }

        return result;
    }

}
