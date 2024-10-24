package edu.uci.ics.amber.operator.sink;

public enum IncrementalOutputMode {
    // sink outputs complete result set snapshot for each update
    SET_SNAPSHOT,
    // sink outputs incremental result set delta for each update
    SET_DELTA
}
