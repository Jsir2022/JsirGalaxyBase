package com.jsirgalaxybase.terminal.client.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import cpw.mods.fml.relauncher.FMLInjectionData;
import net.minecraftforge.common.config.Configuration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TerminalPreferencesRepositoryTest {
    private static File previousMinecraftHome;

    @Rule public final TemporaryFolder temporary=new TemporaryFolder();

    @BeforeClass public static void provideForgeConfigurationHome() throws Exception{
        Field field=FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        previousMinecraftHome=(File)field.get(null);
        field.set(null,new File("."));
    }

    @AfterClass public static void restoreForgeConfigurationHome() throws Exception{
        Field field=FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        field.set(null,previousMinecraftHome);
    }

    @Test public void savesWindowAndMotionPreferencesWithoutLegacySizingOrFontKeys() throws Exception{
        TerminalPreferencesRepository repository=new TerminalPreferencesRepository(temporary.getRoot());
        TerminalPreferences expected=TerminalPreferences.defaults().withWindowSize(TerminalWindowSize.LARGE).withReducedMotion(true);
        repository.save(expected);
        assertEquals(expected,repository.load());
        String text=readConfig();
        assertFalse(text.contains("fontSource"));
        assertFalse(text.contains("density"));
    }

    @Test public void legacyDensityAndFontSourceAreRemoved() throws Exception{
        File file=configFile();file.getParentFile().mkdirs();
        Configuration config=new Configuration(file);config.load();
        config.get("terminal_ui2","density","COMFORTABLE").set("COMFORTABLE");
        config.get("terminal_ui2","fontSource","TERMINAL").set("TERMINAL");
        config.save();
        new TerminalPreferencesRepository(temporary.getRoot()).load();
        assertFalse(readConfig().contains("density"));
        assertFalse(readConfig().contains("fontSource"));
    }

    private String readConfig() throws Exception{return new String(Files.readAllBytes(configFile().toPath()),StandardCharsets.UTF_8);}
    private File configFile(){return new File(new File(temporary.getRoot(),"config"),"jsirgalaxybase-terminal-ui.cfg");}
}
