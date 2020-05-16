package com.enjin.enjincraft.translations;

import com.opencsv.CSVReader;
import lombok.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileConverter {

    // Constants
    public static final String YAML_INDENT = "  ";
    // Keys
    public static final String INTERNAL_KEY = "internal";
    public static final String LOCALE_KEY = "internal.locale";
    public static final String LANGUAGE_KEY = "internal.language";
    public static final String VERSION_KEY = "internal.version";

    private final Map<String, Translation> translationMap = new HashMap<>();

    public FileConverter() {
    }

    public static void main(String... args) throws Exception {
        new FileConverter().generate();
    }

    public void generate() throws Exception {
        for (Locale locale : Locale.values()) {
            String filename = String.format("/%s.csv", locale.name());
            File file = new File(getClass().getResource(filename).toURI());

            Translation translation = readFromCSV(file);
            if (translation != null)
                translationMap.put(translation.getLocale(), translation);
        }

        for (Map.Entry<String, Translation> entry : translationMap.entrySet()) {
            String key = entry.getKey();
            Translation translation = entry.getValue();

            File file = new File(String.format("./%s.yml", key));
            if (file.createNewFile())
                writeToYAML(file, translation);
        }
    }

    private Translation readFromCSV(@NonNull File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        InputStreamReader fr = new InputStreamReader(in, StandardCharsets.UTF_8);
        CSVReader csv = new CSVReader(fr);
        Iterator<String[]> iter = csv.iterator();

        if (!iter.hasNext()) {
            System.err.println("Translation file has no data!");
            return null;
        }

        String[] header = iter.next();
        int translationCol = header.length - 1;

        String locale = null;
        String language = null;
        Integer version = null;
        Translation translation = null;
        while (iter.hasNext()) {
            String[] line = iter.next();

            if (line[0].startsWith(INTERNAL_KEY)) {
                switch (line[0]) {
                    case LOCALE_KEY:
                        locale = line[translationCol];
                        break;
                    case LANGUAGE_KEY:
                        language = line[translationCol];
                        break;
                    case VERSION_KEY:
                        version = Integer.parseInt(line[translationCol]);
                        break;
                    default:
                        break;
                }
            } else {
                if (translation == null)
                    translation = new Translation(locale, language, version);

                translation.addEntry(line[0], line[translationCol]);
            }
        }

        csv.close();
        fr.close();
        return translation;
    }

    private void writeToYAML(@NonNull File file, @NonNull Translation translation) throws Exception {
        Locale locale = Locale.of(translation.getLocale());

        if (locale == null) {
            System.err.println(String.format("Locale %s has not been added yet!", translation.getLocale()));
            return;
        }

        FileOutputStream out = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(out, locale.charset());

        int internalLength = String.format("%s.", INTERNAL_KEY).length(); // Used to format internal sub-keys

        writer.write(toYAMLFormat(INTERNAL_KEY, null, 0));
        writer.write(toYAMLFormat(LOCALE_KEY.substring(internalLength), translation.getLocale(), 1));
        writer.write(toYAMLFormat(LANGUAGE_KEY.substring(internalLength), translation.getLanguage(), 1));
        writer.write(toYAMLFormat(VERSION_KEY.substring(internalLength), translation.getVersion(), 1));

        TranslationEntry root = translation.getRoot();

        for (Map.Entry<String, TranslationEntry> child : root.getEntries().entrySet()) {
            writeToYAML(writer, child.getValue(), 0);
        }

        writer.close();
        out.close();
    }

    private void writeToYAML(Writer writer, TranslationEntry entry, int depth) throws Exception {
        if (entry == null)
            return;

        Map<String, TranslationEntry> entries = entry.getEntries();

        if (entries.size() > 0) {
            writer.write(toYAMLFormat(entry.getKey(), null, depth));

            for (Map.Entry<String, TranslationEntry> child : entries.entrySet()) {
                writeToYAML(writer, child.getValue(), depth + 1);
            }
        } else {
            writer.write(toYAMLFormat(entry.getKey(), entry.getEntry(), depth));
        }
    }

    private String toYAMLFormat(String key, Object value, int indents) {
        if (value instanceof String)
            value = String.format("\"%s\"", value);

        return String.format("%s%s:%s\n",
                indent(YAML_INDENT, indents),
                key,
                value == null ? "" : String.format(" %s", value.toString()));
    }

    private String indent(String indent, int amount) {
        StringBuilder builder = new StringBuilder(indent.length() * amount);

        for (int i = 0; i < amount; i++) {
            builder.append(indent);
        }

        return builder.toString();
    }

}
