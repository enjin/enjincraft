package com.enjin.enjincraft.spigot.cmd.arg;

import com.enjin.enjincraft.spigot.i18n.Locale;
import com.enjin.enjincraft.spigot.i18n.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocaleArgumentProcessor extends AbstractArgumentProcessor<Locale> {

    public static final LocaleArgumentProcessor INSTANCE = new LocaleArgumentProcessor();

    @Override
    public List<String> tab(CommandSender sender, String arg) {
        String lowerCaseArg = arg.toLowerCase();
        return Arrays.asList(Locale.values()).stream()
                .map(locale -> sender instanceof Player ? Translation.localeNames().get(locale)
                        : locale.locale())
                .filter(locale -> locale.toLowerCase().startsWith(lowerCaseArg))
                .collect(Collectors.toList());
    }

    @Override
    public <A> Optional<Locale> parse(CommandSender sender, String arg) {
        Optional result = Optional.empty();

        for (Locale locale : Locale.values()) {
            if (locale.locale().equalsIgnoreCase(arg) || Translation.localeNames().get(locale).equalsIgnoreCase(arg)) {
                result = Optional.of(locale);
                break;
            }
        }

        return result;
    }

}
