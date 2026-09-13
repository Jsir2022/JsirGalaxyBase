package com.jsirgalaxybase.ui2.demo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Build gate for the production terminal entries included in the shared visual report. */
public final class TerminalVisualVerifier {
    private TerminalVisualVerifier() {}

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "build/ui-lab" : args[0]);
        UiLabExporter.exportAll(output);
        File[] audits = output.listFiles();
        int checked = 0;
        if (audits != null) for (File audit : audits) {
            if (!audit.getName().startsWith("terminal-") || !audit.getName().endsWith(".ui2.txt")) continue;
            String value = new String(Files.readAllBytes(audit.toPath()), StandardCharsets.UTF_8);
            if (!value.contains("SUMMARY|issues=0")) {
                throw new IllegalStateException("terminal visual audit failed: " + audit.getName());
            }
            if (hasCollapsedContent(value)) {
                throw new IllegalStateException("terminal visual contains collapsed mounted content: " + audit.getName());
            }
            if ((audit.getName().contains("market-detail-") || audit.getName().contains("market-single-"))
                && !value.contains("CHART_LINE|")) {
                throw new IllegalStateException("market data produced no visible chart geometry: " + audit.getName());
            }
            if (audit.getName().contains("market-empty-") && !value.contains("暂无成交")) {
                throw new IllegalStateException("empty market chart has no explicit empty state: " + audit.getName());
            }
            checked++;
        }
        if (checked == 0) throw new IllegalStateException("no terminal visual scenarios were generated");
        System.out.println("Verified " + checked + " terminal visual snapshots in " + output.getAbsolutePath());
    }

    private static boolean hasCollapsedContent(String snapshot) {
        for (String line : snapshot.split("\\n")) {
            if (line.contains("terminal-nav-spacer")) continue;
            if (line.matches(".*\\|[-0-9]+,[-0-9]+ (?:0x[0-9]+|[0-9]+x0)\\|mounted")) return true;
        }
        return false;
    }
}
