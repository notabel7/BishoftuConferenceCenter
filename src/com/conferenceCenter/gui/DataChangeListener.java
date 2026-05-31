package com.conferenceCenter.gui;

/**
 * Observer interface that lets sibling panels notify each other when their
 * underlying data changes, so every view stays in sync without a manual refresh.
 *
 * Real-world pattern: used by systems like Ungerboeck (Momentus) and EventPro
 * where resource changes (staff added, hall edited) are immediately reflected
 * in every module that references those resources.
 */
@FunctionalInterface
public interface DataChangeListener {
    /** Called on the Event Dispatch Thread after a panel saves, edits, or deletes data. */
    void onDataChanged();
}
