package com.example.profiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

public class LinuxPerfLockProfiler implements ExternalProfiler {

    private static final String PERF_EXEC = "perf";

    protected final TempFile perfBinData;
    protected final TempFile perfReportData;

    public LinuxPerfLockProfiler(final String initLine) throws ProfilerException {
        try {
            perfBinData = FileUtils.weakTempFile("perf-lock_bin");
            perfReportData = FileUtils.weakTempFile("perf-lock_report");
        } catch (IOException e) {
            throw new ProfilerException(e);
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(final BenchmarkParams params) {
        return List.of(PERF_EXEC, "lock", "record", "--output", perfBinData.getAbsolutePath());
    }

    @Override
    public Collection<String> addJVMOptions(final BenchmarkParams params) {
        return List.of();
    }

    @Override
    public void beforeTrial(final BenchmarkParams benchmarkParams) {
        // nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(final BenchmarkResult br,
                                                   final long pid,
                                                   final File stdOut,
                                                   final File stdErr) {
        createReport();
        TextResult result = createResult();

        perfBinData.delete();
        perfReportData.delete();

        return Collections.singleton(result);
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Linux 'perf lock' profiler. Creates record, then prints report";
    }

    private TextResult process() {
        createReport();
        return createResult();
    }

    private TextResult createResult() {
        try (final FileReader fr = new FileReader(perfReportData.file());
             final BufferedReader reader = new BufferedReader(fr)) {

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            String line;
            while ((line = reader.readLine()) != null) {
                pw.println(line);
            }

            pw.flush();
            sw.flush();

            final String result = sw.toString();
            return new TextResult(result, "perf lock report");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createReport() {
        try (final FileOutputStream fos = new FileOutputStream(perfReportData.file())) {
            final ProcessBuilder pb = new ProcessBuilder(PERF_EXEC, "lock", "report", "--input", perfBinData.getAbsolutePath());
            final Process p = pb.start();

            final InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fos);
            final InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fos);

            errDrainer.start();
            outDrainer.start();

            p.waitFor();

            errDrainer.join();
            outDrainer.join();
        } catch (final IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
