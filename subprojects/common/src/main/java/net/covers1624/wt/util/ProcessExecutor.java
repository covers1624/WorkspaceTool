/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A simple wrapper around Java's ProcessBuilder / Process system,
 * with a better interface for IO.
 *
 * Created by covers1624 on 12/01/19.
 */
public class ProcessExecutor {

    private List<String> command = new ArrayList<>();
    private File directory;
    private Map<String, String> envVars = new HashMap<>();
    private IO io = new IO();

    private List<Consumer<ProcessExecutor>> preStartCallbacks = new ArrayList<>();

    /**
     * Construct a blank one.
     */
    public ProcessExecutor() {
    }

    /**
     * Construct from an existing ProcessBuilder.
     * Only copies the command, working dir, and envVars.
     *
     * @param from The ProcessBuilder
     */
    public ProcessExecutor(ProcessBuilder from) {
        command.addAll(from.command());
        directory = from.directory();
        envVars.putAll(from.environment());
    }

    /**
     * Adds a single argument to the command.
     *
     * @param arg The arg.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor addCmdArg(String arg) {
        command.add(arg);
        return this;
    }

    /**
     * Adds an array of args to the command.
     *
     * @param args The args.
     * @return The same ProcessExecutor
     */
    public ProcessExecutor addCmdArgs(String... args) {
        return addCmdArgs(Arrays.asList(args));
    }

    /**
     * Adds a List of args to the command.
     *
     * @param args The args.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor addCmdArgs(List<String> args) {
        command.addAll(args);
        return this;
    }

    /**
     * Sets the command to the specified array of args.
     *
     * @param args The args.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor setCmd(String... args) {
        return setCmd(Arrays.asList(args));
    }

    /**
     * Sets the command to the specified List of args.
     *
     * @param args The args.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor setCmd(List<String> args) {
        command.clear();
        command.addAll(args);
        return this;
    }

    /**
     * Gets an unmodifiable list of command arguments.
     *
     * @return The args.
     */
    public List<String> getCmd() {
        return Collections.unmodifiableList(command);
    }

    /**
     * Sets the working directory for the ProcessExecutor.
     *
     * @param dir The dir.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor setWorkingDir(File dir) {
        directory = dir;
        return this;
    }

    /**
     * Gets the working directory for the ProcessExecutor.
     *
     * @return The working directory.
     */
    public File getWorkingDir() {
        return directory;
    }

    /**
     * Adds an EnvVar to the ProcessExecutor.
     *
     * @param key   The key.
     * @param value The value.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor addEnvVar(String key, String value) {
        envVars.put(key, value);
        return this;
    }

    /**
     * Adds a Map of env vars to the ProcessExecutor.
     *
     * @param envVars The env vars.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor addEnvVars(Map<String, String> envVars) {
        this.envVars.putAll(envVars);
        return this;
    }

    /**
     * Sets the Env vars for the ProcessExecutor
     *
     * @param envVars The env vars.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor setEnvVars(Map<String, String> envVars) {
        this.envVars.clear();
        this.envVars.putAll(envVars);
        return this;
    }

    /**
     * Gets this ProcessExecutors env vars.
     *
     * @return The vars.
     */
    public Map<String, String> getEnvVars() {
        return Collections.unmodifiableMap(envVars);
    }

    /**
     * Adds a callback that will be executed just before the Process is started.
     *
     * @param callback The callback.
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor addPreStartCallback(Consumer<ProcessExecutor> callback) {
        preStartCallbacks.add(callback);
        return this;
    }

    /**
     * Clears all callbacks.
     *
     * @return The same ProcessExecutor.
     */
    public ProcessExecutor clearCallbacks() {
        preStartCallbacks.clear();
        return this;
    }

    /**
     * Gets the IO holder for this ProcessExecutor.
     *
     * @return The IO holder.
     */
    public IO getIO() {
        return io;
    }

    /**
     * Starts the process with the current state of this ProcessExecutor.
     *
     * @return The RunningProcess.
     * @throws ProcessException If the Process could not be started.
     */
    public RunningProcess start() throws ProcessException {
        RunningProcess proc = new RunningProcess(this);
        proc.start();
        return proc;
    }

    //Internal.
    private ProcessBuilder toBuilder() {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        builder.directory(directory);
        builder.environment().putAll(envVars);
        return builder;
    }

    /**
     * Copies the ProcessExecutor.
     * Only copies the command arguments, working dir, env vars and callbacks.
     * IO is completely stock on the new ProcessExecutor.
     *
     * @return The new ProcessExecutor.
     */
    public ProcessExecutor clone() {
        ProcessExecutor other = new ProcessExecutor();
        other.setCmd(getCmd());
        other.setWorkingDir(getWorkingDir());
        other.setEnvVars(getEnvVars());
        other.preStartCallbacks.addAll(preStartCallbacks);
        return other;
    }

    /**
     * Represents a Process that has been started and is or was running.
     */
    public static class RunningProcess {

        private final ProcessExecutor executor;
        private final ProcessBuilder builder;
        private final IO io;
        private Process process;
        private IOThread stdOutThread;
        private IOThread stdErrThread;
        private IOThread stdInThread;

        private RunningProcess(ProcessExecutor executor) {
            this.executor = executor;
            builder = executor.toBuilder();
            io = executor.getIO();
        }

        private void start() throws ProcessException {
            try {
                executor.preStartCallbacks.forEach(e -> e.accept(executor));
                process = builder.start();
                IO io = this.io.copyInternal();
                InputStream stdOut = process.getInputStream();
                InputStream stdErr = process.getErrorStream();
                OutputStream stdIn = process.getOutputStream();
                if (io.stdOutConsumer != null) {
                    spawnIOThread(() -> io.stdOutConsumer.accept(stdOut));
                }
                if (io.stdErrConsumer != null) {
                    spawnIOThread(() -> io.stdErrConsumer.accept(stdErr));
                }
                if (io.stdInConsumer != null) {
                    spawnIOThread(() -> io.stdInConsumer.accept(stdIn));
                }
            } catch (IOException e) {
                throw new ProcessException(e, "Failed to start process.");
            }
        }

        private void spawnIOThread(ThrowingRunnable<IOException> action) {
            IOThread ioThread = new IOThread(process, action);
            ioThread.setDaemon(true);
            ioThread.setName("ProcessExecutor-IOThread");
            ioThread.start();
        }

        /**
         * @return If this Process is currently running.
         */
        public boolean isAlive() {
            return process.isAlive();
        }

        /**
         * Waits for this Process to finish executing.
         */
        public void waitFor() {
            waitFor(false);
        }

        /**
         * Waits for this Process to finish executing, or times out.
         * See {@link Process#waitFor(long, TimeUnit)} for more information.
         *
         * @param timeout The timeout.
         * @param unit    The units.
         */
        public void waitFor(long timeout, TimeUnit unit) {
            waitFor(false, timeout, unit);
        }

        /**
         * Waits for the process to finish executing,
         * optionally asserts the exit code for the process should be zero.
         *
         * @param assertZeroExit If the exit code should be asserted.
         */
        public void waitFor(boolean assertZeroExit) {
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
            }
            if (assertZeroExit) {
                assertZeroExit();
            }
        }

        /**
         * Waits for this Process to finish executing, or times out.
         * See {@link Process#waitFor(long, TimeUnit)} for more information.
         * Optionally assets the exit code for the process should be zero.
         *
         * @param assertZeroExit If the exit code should be asserted.
         * @param timeout        The timeout.
         * @param unit           The units.
         */
        public void waitFor(boolean assertZeroExit, long timeout, TimeUnit unit) {
            try {
                process.waitFor(timeout, unit);
            } catch (InterruptedException ignored) {
            }
            if (assertZeroExit) {
                assertZeroExit();
            }
        }

        /**
         * Gets the exit code for the process.
         * The underlying Process implementation may choose to throw
         * an exception if the Process is not finished, this is undefined
         * as it is platform specific.
         *
         * @return The exit code for the process.
         */
        public int getExitCode() {
            return process.exitValue();
        }

        private void assertZeroExit() {
            int code = getExitCode();
            if (code != 0) {
                throw new ProcessException("Non zero exit code: " + code);
            }
        }
    }

    /**
     * Generic exception for ProcessExecutor.
     */
    public static class ProcessException extends RuntimeException {

        public ProcessException(Throwable t, String str, Object... args) {
            super(ParameterFormatter.format(str, args), t);
        }

        public ProcessException(String str, Object... args) {
            super(ParameterFormatter.format(str, args));
        }
    }

    /**
     * The IO holder for a ProcessExecutor instance.
     */
    public static class IO {

        private final String newLine = System.getProperty("line.separator");

        private ThrowingConsumer<InputStream, IOException> stdOutConsumer;
        private ThrowingConsumer<InputStream, IOException> stdErrConsumer;
        private ThrowingConsumer<OutputStream, IOException> stdInConsumer;

        private IO() {
            pipeStdOut(System.out);
            pipeStdErr(System.err);
        }

        /**
         * Sets the stdOut of the Executed process to be piped into the supplied OutputStream.
         *
         * @param out The OutputStream.
         * @return The same IO instance.
         */
        public IO pipeStdOut(OutputStream out) {
            stdOutConsumer = o -> Utils.copy(o, out);
            return this;
        }

        /**
         * Sets the stdErr of the Executed process to be piped into the supplied OutputStream.
         *
         * @param out The OutputStream.
         * @return The same IO instance.
         */
        public IO pipeStdErr(OutputStream out) {
            stdErrConsumer = o -> Utils.copy(o, out);
            return this;
        }

        /**
         * Sets the stdIn of the Executed process to be piped in from the supplied InputStream.
         *
         * @param in The InputStream.
         * @return The same IO instance.
         */
        public IO pipeStdIn(InputStream in) {
            stdInConsumer = o -> Utils.copy(in, o);
            return this;
        }

        /**
         * Passes each line of the Executed processes stdOut to the supplied Consumer.
         *
         * @param cons The consumer.
         * @return The same IO instance.
         */
        public IO consumeStdOutLines(Consumer<String> cons) {
            stdOutConsumer = makeLineConsumer(cons);
            return this;
        }

        /**
         * Passes each line of the Executed processes stdErr to the supplied Consumer.
         *
         * @param cons The consumer.
         * @return The same IO instance.
         */
        public IO consumeStdErrLines(Consumer<String> cons) {
            stdErrConsumer = makeLineConsumer(cons);
            return this;
        }

        /**
         * Returns a StringBuilder that is automatically updated from the Processes stdOut during
         * the runtime of the process, This should be called before executing the Process then checked
         * once execution has finished.
         *
         * ALL Process IO is performed off the 'Main' thread, just be smart.
         *
         * @return The StringBuilder.
         */
        public StringBuilder consumeStdOut() {
            StringBuilder builder = new StringBuilder();
            consumeStdOutLines(e -> {
                builder.append(e);
                builder.append(newLine);
            });
            return builder;
        }

        /**
         * Returns a StringBuilder that is automatically updated from the Processes stdErr during
         * the runtime of the process, This should be called before executing the Process then checked
         * once execution has finished.
         *
         * ALL Process IO is performed off the 'Main' thread, just be smart.
         *
         * @return The StringBuilder.
         */
        public StringBuilder consumeStdErr() {
            StringBuilder builder = new StringBuilder();
            consumeStdErrLines(e -> {
                builder.append(e);
                builder.append(newLine);
            });
            return builder;
        }

        /**
         * Sends stdOut to the 4th dimension where puppies sleep with kittens.
         *
         * @return The same IO instance.
         */
        public IO voidStdOut() {
            stdOutConsumer = null;
            return this;
        }

        /**
         * Sends stdErr to the 4th dimension where puppies sleep with kittens.
         *
         * @return The same IO instance.
         */
        public IO voidStdErr() {
            stdErrConsumer = null;
            return this;
        }

        /**
         * Literally passes nothing to the Executed process.
         *
         * @return The same IO instance.
         */
        public IO voidStdIn() {
            stdInConsumer = null;
            return this;
        }

        private ThrowingConsumer<InputStream, IOException> makeLineConsumer(Consumer<String> cons) {
            return o -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(o));
                String line;
                while ((line = reader.readLine()) != null) {
                    cons.accept(line);
                }
            };
        }

        private IO copyInternal() {
            IO copy = new IO();
            copy.stdOutConsumer = stdOutConsumer;
            copy.stdErrConsumer = stdErrConsumer;
            copy.stdInConsumer = stdInConsumer;
            return copy;
        }
    }

    /**
     * Simple thread for handling Process IO.
     */
    private static class IOThread extends Thread {

        private final Process process;
        private final ThrowingRunnable<IOException> action;

        private IOThread(Process process, ThrowingRunnable<IOException> action) {
            this.process = process;
            this.action = action;
        }

        @Override
        public void run() {
            while (!isInterrupted() && process.isAlive()) {
                try {
                    action.run();
                } catch (IOException e) {
                    interrupt();
                }
            }
        }

    }
}
