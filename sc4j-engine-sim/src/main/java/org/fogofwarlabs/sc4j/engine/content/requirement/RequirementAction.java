package org.fogofwarlabs.sc4j.engine.content.requirement;

/**
 * Names what a player asks to do when a requirement is checked; it decides the content kind of the checked type index.
 */
public enum RequirementAction {
    /**
     * Build, train or morph a unit type; the type index is a unit type.
     */
    BUILD_UNIT,
    /**
     * Research the next level of an upgrade; the type index is an upgrade type.
     */
    RESEARCH_UPGRADE,
    /**
     * Research a tech; the type index is a tech type.
     */
    RESEARCH_TECH,
    /**
     * Use a researched tech; the type index is a tech type.
     */
    USE_TECH,
    /**
     * Issue an order; the type index is an order type.
     */
    ISSUE_ORDER
}
