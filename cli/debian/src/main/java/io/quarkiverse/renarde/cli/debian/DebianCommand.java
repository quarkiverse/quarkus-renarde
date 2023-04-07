package io.quarkiverse.renarde.cli.debian;

import java.util.concurrent.Callable;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@TopCommand
@Command(name = "debian", sortOptions = false, mixinStandardHelpOptions = true, header = "Create or build a Debian package", subcommands = {
        Create.class, Build.class })
public class DebianCommand implements Callable<Integer> {

    @Spec
    protected CommandSpec spec;

    @Override
    public Integer call() {
        // print usage?
        return ExitCode.USAGE;
    }

}
