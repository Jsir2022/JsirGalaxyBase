package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.Test;

public class BqBaseOnlyPreflightTest {
    @Test
    public void acceptsOnlyExplicitLoopbackTestDatabases() {
        BqBaseOnlyPreflight.requireIsolatedLoopbackUrl("jdbc:postgresql://127.0.0.1:55436/quest_it",
            "ISOLATED_ONLY");
        BqBaseOnlyPreflight.requireIsolatedLoopbackUrl("jdbc:postgresql://localhost/galaxy_base_test",
            "ISOLATED_ONLY");
        assertRejected("jdbc:postgresql://db.example.org/quest_it", "ISOLATED_ONLY");
        assertRejected("jdbc:postgresql://127.0.0.1/galaxy-base", "ISOLATED_ONLY");
        assertRejected("jdbc:postgresql://127.0.0.1/quest", "ISOLATED_ONLY");
        assertRejected("jdbc:postgresql://127.0.0.1/quest_it?currentSchema=public", "ISOLATED_ONLY");
        assertRejected("jdbc:postgresql://127.0.0.1/quest_it", "");
        assertRejected("jdbc:mysql://127.0.0.1/quest_it", "ISOLATED_ONLY");
    }

    @Test
    public void validatesJarIdentityAndRejectsBundledTooling() throws Exception {
        Path jar = temporaryJar("valid-base", "com/jsirgalaxybase/GalaxyBase.class",
            "com/jsirgalaxybase/modules/quest/QuestModule.class", "META-INF/betterquesting-provenance.md");
        String digest = sha256(jar);
        BqBaseOnlyPreflight.ArtifactReport report = BqBaseOnlyPreflight.verifyArtifact(jar, digest);
        assertEquals(digest, report.getSha256());
        assertEquals(2, report.getClassCount());

        try {
            BqBaseOnlyPreflight.verifyArtifact(jar, repeat('0', 64));
            fail("mismatched JAR digest must be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("SHA-256 mismatch"));
        }

        Path bundledImporter = temporaryJar("base-with-importer", "com/jsirgalaxybase/GalaxyBase.class",
            "com/jsirgalaxybase/modules/quest/QuestModule.class",
            "com/jsirgalaxybase/quest/bqimport/BqMigrationMain.class");
        try {
            BqBaseOnlyPreflight.verifyArtifact(bundledImporter, sha256(bundledImporter));
            fail("offline importer classes must not enter Base runtime JAR");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("forbidden"));
        }
    }

    @Test
    public void rejectsMissingRuntimeClassesAndMalformedExpectedDigest() throws Exception {
        Path missingQuestModule = temporaryJar("base-without-quest", "com/jsirgalaxybase/GalaxyBase.class");
        try {
            BqBaseOnlyPreflight.verifyArtifact(missingQuestModule, sha256(missingQuestModule));
            fail("missing Quest runtime must be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("QuestModule"));
        }
        try {
            BqBaseOnlyPreflight.verifyArtifact(missingQuestModule, "not-a-digest");
            fail("malformed expected digest must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("64 hex"));
        }
    }

    private static void assertRejected(String url, String confirmation) {
        try {
            BqBaseOnlyPreflight.requireIsolatedLoopbackUrl(url, confirmation);
            fail("unsafe preflight database URL accepted: " + url);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isEmpty());
        }
    }

    private static Path temporaryJar(String prefix, String... entries) throws Exception {
        Path jar = Files.createTempFile(prefix, ".jar");
        try (OutputStream output = Files.newOutputStream(jar); JarOutputStream archive = new JarOutputStream(output)) {
            for (String entry : entries) {
                archive.putNextEntry(new JarEntry(entry));
                if (entry.endsWith(".class")) archive.write(new byte[] { 0 });
                else archive.write("provenance only".getBytes("UTF-8"));
                archive.closeEntry();
            }
        }
        jar.toFile().deleteOnExit();
        return jar;
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(path));
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
