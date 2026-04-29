package com.example.ojpt.judge;

public record CommandResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut
) {
    public static CommandResult success(String stdout) {
        return new CommandResult(0, stdout, "", false);
    }

    public static CommandResult failure(String message) {
        return new CommandResult(-1, "", message, false);
    }

    public static CommandResult timeout() {
        return new CommandResult(-1, "", "Command timed out", true);
    }
}
