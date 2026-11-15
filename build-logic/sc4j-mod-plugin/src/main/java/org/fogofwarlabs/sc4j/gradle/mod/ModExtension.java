package org.fogofwarlabs.sc4j.gradle.mod;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

/**
 * Declares a mod of the SC4J engine: the extension {@code sc4jMod} of the mod plugin {@code org.fogofwarlabs.sc4j.mod}.
 * {@snippet lang = "groovy":
 * plugins {
 *     id 'org.fogofwarlabs.sc4j.mod' version '<engine version>'
 * }
 *
 * sc4jMod {
 *     id = 'my-mod'
 * }
 *}
 * The plugin compiles the source sets against the engine API of its own version ({@code org.fogofwarlabs.sc4j:sc4j-engine}) and adds these
 * tasks:
 * <ul>
 *     <li>{@code modJar}: packs the classes and resources of the source sets into {@code build/mods/<id>.jar} without file times in a fixed
 *     order, with the licence files in {@code META-INF/}, and checks that its manifest {@code sc4j-mod.json} names the mod id and an entry
 *     class of the jar.</li>
 *     <li>{@code checkApiUse}: fails when the Java sources of the source sets use an internal element of the engine; {@code check} runs
 *     it.</li>
 *     <li>{@code runMod}: runs the mod jar in the engine runtime of the plugin's version, with the user data of the install or of
 *     {@code build/sc4j-run}; {@code --args} passes runtime options (such as {@code --headless}) and {@code --debug-jvm} waits for a
 *     debugger.</li>
 *     <li>{@code devInstall}: copies the mod jar into {@code mods/} of the install.</li>
 * </ul>
 * The variant of the usage {@code sc4j-mod-jar} ({@code modJarElements}) gives the mod jar to other projects of the build.
 */
public abstract class ModExtension {
    /**
     * Creates a {@link ModExtension} instance.
     */
    public ModExtension() {
    }

    /**
     * Returns the mod id: the {@code id} of the manifest and the name of the mod jar ({@code <id>.jar}). It must be set.
     *
     * @return the mod id
     */
    public abstract @NotNull Property<@NotNull String> getId();

    /**
     * Returns the source sets whose classes and resources make up the mod jar and whose Java sources {@code checkApiUse} scans. The source
     * sets of this project among them compile against the engine API. The default is the main source set.
     *
     * @return the source sets of the mod
     */
    public abstract @NotNull ListProperty<@NotNull SourceSet> getSourceSets();

    /**
     * Returns the SC4J install folder that {@code devInstall} copies the mod jar into and whose user data {@code runMod} uses. The default is
     * the Gradle property {@code sc4j.install}.
     *
     * @return the install folder
     */
    public abstract @NotNull DirectoryProperty getInstall();

    /**
     * Returns the licence files that the mod jar carries in {@code META-INF/}. The default is the files {@code LICENSE} and {@code NOTICE}
     * of the root project folder that exist.
     *
     * @return the licence files
     */
    public abstract @NotNull ConfigurableFileCollection getLicenses();
}
