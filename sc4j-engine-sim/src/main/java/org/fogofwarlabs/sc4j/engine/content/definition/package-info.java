/**
 * Defines the immutable definitions of the content types: the initial values a game copies into its tables.
 * <p>
 * Each definition is built once, while the mod registers, with its nested builder; references to other content are
 * identities that must be registered in the same content. The trait and target enums name the bits of the mask columns.
 */
package org.fogofwarlabs.sc4j.engine.content.definition;
