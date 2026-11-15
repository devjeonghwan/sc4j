/**
 * Defines the content type identities and the registries that give them their registration-order indices.
 * <p>
 * An identity ({@link org.fogofwarlabs.sc4j.engine.content.type.UnitType} and the other final subclasses of
 * {@link org.fogofwarlabs.sc4j.engine.content.type.ContentType}) holds no values: it is a constant a mod declares and
 * refers to in source. A {@link org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry} assigns the index when the
 * identity is registered and freezes it with the registry; a
 * {@link org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry} also keeps the definition at that index. A mod
 * may define its own identity kinds by extending {@code ContentType}.
 */
package org.fogofwarlabs.sc4j.engine.content.type;
