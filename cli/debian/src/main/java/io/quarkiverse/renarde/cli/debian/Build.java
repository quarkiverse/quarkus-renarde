package io.quarkiverse.renarde.cli.debian;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

@Command(name = "build", description = "Build a Debian package", mixinStandardHelpOptions = true)
public class Build implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.err.println("Building Debian package");
        return ExitCode.OK;
    }

}
