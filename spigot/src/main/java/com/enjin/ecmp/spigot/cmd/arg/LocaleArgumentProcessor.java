package com.enjin.ecmp.spigot.cmd.arg;

import com.enjin.ecmp.spigot.i18n.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocaleArgumentProcessor extends AbstractArgumentProcessor<Locale> {

    public static final LocaleArgumentProcessor INSTANCE = new LocaleArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        return Arrays.asList(Locale.values()).stream()
                .map(sender instanceof Player ? Locale::language : Locale::locale)
                .collect(Collectors.toList());
    }

    @Override
    public <A> Optional<Locale> parse(CommandSender sender, String arg) {
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
