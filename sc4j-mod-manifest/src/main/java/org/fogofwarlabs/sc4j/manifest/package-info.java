/**
 * Reads mod jars without loading their classes: the manifest {@code sc4j-mod.json} ({@link org.fogofwarlabs.sc4j.manifest.ModManifest}),
 * the images it names and the reading failures ({@link org.fogofwarlabs.sc4j.manifest.ManifestException}).
 * <p>
 * The launcher and the runtime share this module; it depends on the generic base only, never on the engine.
 */
package org.fogofwarlabs.sc4j.manifest;
