package dev.aoqia.leaf.filament;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.aoqia.leaf.filament.task.*;
import dev.aoqia.leaf.filament.task.base.WithFileOutput;
import dev.aoqia.leaf.loom.LoomGradlePlugin;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidVersionMeta;
import dev.aoqia.leaf.loom.util.gradle.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public final class FilamentGradlePlugin implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void apply(Project project) {
        final FilamentExtension extension = project.getExtensions()
            .create("filament", FilamentExtension.class);
        final TaskContainer tasks = project.getTasks();

        var metaProvider = extension.getZomboidVersionMetadata();

        tasks.register("getOfficialJar", GetOfficialJarTask.class, task -> {
            final String jarName = "zomboid-CLIENT-only.jar";

            task.getGradleUserHomeDir().set(project.getGradle().getGradleUserHomeDir().toString());
            task.getJarName().set(jarName);
            task.getZomboidVersion().set(extension.getZomboidVersion().get());
            task.getFilamentCacheDir().set(extension.getZomboidDirectory().get());
            task.getForceCopy().set(project.getGradle().getStartParameter().isRefreshDependencies()
                                    || Boolean.getBoolean("loom.refresh"));
            task.getOutput().set(extension.getZomboidFile(jarName));
        });

        tasks.register("generatePackageInfoMappings", GeneratePackageInfoMappingsTask.class);
        tasks.register("javadocLint", JavadocLintTask.class);

        var combineUnpickDefinitions = tasks.register("combineUnpickDefinitions",
            CombineUnpickDefinitionsTask.class);
        tasks.register("remapUnpickDefinitionsOfficial", RemapUnpickDefinitionsTask.class, task -> {
            task.dependsOn(combineUnpickDefinitions);
            task.getInput()
                .set(combineUnpickDefinitions.flatMap(CombineUnpickDefinitionsTask::getOutput));
            task.getSourceNamespace().set("named");
            task.getTargetNamespace().set("official");
        });

        var cleanFilament = tasks.register("cleanFilament",
            Delete.class, task -> task.delete(extension.getCacheDirectory()));
        tasks.named("clean", task -> task.dependsOn(cleanFilament));

        try {
            var zomboidLibraries = project.getConfigurations().getByName("zomboidLibraries");

            GradleUtils.afterSuccessfulEvaluation(project, () -> {
                var name = zomboidLibraries.getName();
                for (Dependency dependency : getDependencies(metaProvider.get(),
                    project.getDependencies())) {
                    project.getDependencies().add(name, dependency);
                }
            });
        } catch (UnknownConfigurationException e) {

            throw new RuntimeException("zomboidLibraries loom configuration not found.");
        }
    }

    private Dependency[] getDependencies(ZomboidVersionMeta meta,
        DependencyHandler dependencyHandler) {
        return meta.libraries().stream()
            .filter(library -> library.artifact() != null)
            .map(library -> dependencyHandler.create(library.name()))
            .toArray(Dependency[]::new);
    }

    private Provider<? extends RegularFile> getOutput(
        TaskProvider<? extends WithFileOutput> taskProvider) {
        return taskProvider.flatMap(WithFileOutput::getOutput);
    }
}
