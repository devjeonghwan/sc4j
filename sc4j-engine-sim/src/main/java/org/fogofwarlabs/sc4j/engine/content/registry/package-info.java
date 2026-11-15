/**
 * Defines how a mod registers its content and the frozen result.
 * <p>
 * {@link org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry} receives the types, definitions, extension
 * columns and modifiers during the registration; building it freezes every registry into a
 * {@link org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition}, which every game of the session creates its
 * tables from.
 */
package org.fogofwarlabs.sc4j.engine.content.registry;
