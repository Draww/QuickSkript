package com.github.stefvanschie.quickskript.parsing;

import com.github.stefvanschie.quickskript.TestClassBase;
import com.github.stefvanschie.quickskript.file.SkriptFile;
import com.github.stefvanschie.quickskript.skript.Skript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A test which asserts that all specified skript files
 * can be parsed without any exceptions being raised.
 */
class SkriptFileParseTest extends TestClassBase {

    @Test
    void test() throws IOException {
        URL resource = getClass().getClassLoader().getResource("skript-file-parse-test");
        File directory = new File(Objects.requireNonNull(resource).getPath());

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            Skript skript = new Skript(SkriptFile.getName(file), SkriptFile.load(file));

            assertDoesNotThrow(skript::registerCommands,
                    "Error while parsing commands of skript file: " + file.getName());

            assertDoesNotThrow(skript::registerEvents,
                    "Error while parsing events of skript file: " + file.getName());
        }
    }
}
