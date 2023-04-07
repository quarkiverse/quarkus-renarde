package io.quarkiverse.renarde.cli.debian;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

@Command(name = "create", description = "Create a Debian package", mixinStandardHelpOptions = true)
public class Create implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.err.println("Creating Debian package");
        return ExitCode.OK;
    }

}
