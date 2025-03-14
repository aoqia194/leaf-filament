package dev.aoqia.leaf.filament.task.enigma;

import javax.inject.Inject;
import java.util.List;

import cuchaz.enigma.command.Command;
import dev.aoqia.leaf.filament.task.base.FilamentTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public abstract class EnigmaCommandTask extends FilamentTask {
    @TaskAction
    public void run() {
        WorkQueue workQueue = getWorkerExecutor().noIsolation();
        workQueue.submit(EnigmaAction.class, parameters -> {
            parameters.getCommandClassName().set(getCommandClass().getName());
            parameters.getArguments().set(getArguments());
        });
    }

    @Internal
    public abstract Class<? extends Command> getCommandClass();

    @Internal
    protected abstract List<String> getArguments();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    public interface EnimgaParameters extends WorkParameters {
        Property<String> getCommandClassName();

        ListProperty<String> getArguments();
    }

    public abstract static class EnigmaAction implements WorkAction<EnimgaParameters> {
        @Override
        public void execute() {
            try {
                Class<?> commandClass = Class.forName(getParameters().getCommandClassName().get());
                Command command = (Command) commandClass.getConstructor().newInstance();
                command.run(getParameters().getArguments().get().toArray(String[]::new));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
