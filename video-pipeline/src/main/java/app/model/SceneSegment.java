package app.model;

public record SceneSegment(String startTime, String endTime, String category) {
    public SceneSegment {
        if (startTime == null || startTime.isBlank()) {
            throw new IllegalArgumentException("Start time must not be blank");
        }
        if (endTime == null || endTime.isBlank()) {
            throw new IllegalArgumentException("End time must not be blank");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
    }
}
