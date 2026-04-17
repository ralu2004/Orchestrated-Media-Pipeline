package app.model;

/** Compliance marker that tags a timeline timestamp for a target region. */
public record ContentFlag(String timestamp, String region) {}