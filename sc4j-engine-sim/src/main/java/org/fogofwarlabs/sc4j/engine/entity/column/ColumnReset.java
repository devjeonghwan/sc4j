package org.fogofwarlabs.sc4j.engine.entity.column;

/**
 * Says what happens to a column value when its pool slot is allocated again.
 */
public enum ColumnReset {
    /**
     * The pool sets the value to the column default when the slot is allocated.
     */
    ON_ALLOCATE,
    /**
     * The value stays as the previous use of the slot left it; the allocating system sets it when it needs to.
     */
    KEEP
}
