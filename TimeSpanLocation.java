public TimeSpanLocation() {
    this.onDutyNonDrivingTime = Duration.ZERO;
}

public TimeSpanLocation(LocalDateTime startTime, LocalDateTime endTime, double latitude, double longitude, boolean isBreak, Duration onDutyNonDrivingTime) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.latitude = latitude;
    this.longitude = longitude;
    this.isBreak = isBreak;
    this.onDutyNonDrivingTime = onDutyNonDrivingTime != null ? onDutyNonDrivingTime : Duration.ZERO;
}

public LocalDateTime getStartTime() {
    return startTime;
}

public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
}

public LocalDateTime getEndTime() {
    return endTime;
}

public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
}

public double getLatitude() {
    return latitude;
}

public void setLatitude(double latitude) {
    this.latitude = latitude;
}

public double getLongitude() {
    return longitude;
}

public void setLongitude(double longitude) {
    this.longitude = longitude;
}

public boolean isBreak() {
    return isBreak;
}

public void setBreak(boolean isBreak) {
    this.isBreak = isBreak;
}

public Duration getOnDutyNonDrivingTime() {
    return onDutyNonDrivingTime;
}

public void setOnDutyNonDrivingTime(Duration onDutyNonDrivingTime) {
    this.onDutyNonDrivingTime = onDutyNonDrivingTime != null ? onDutyNonDrivingTime : Duration.ZERO;
}

public Duration getDrivingDuration() {
    return isBreak ? Duration.ZERO : Duration.between(startTime, endTime);
}

public Duration getTotalOnDutyDuration() {
    return getDrivingDuration().plus(onDutyNonDrivingTime);
}

public void shiftBy(Duration duration) {
    this.startTime = startTime.plus(duration);
    this.endTime = endTime.plus(duration);
}

@Override
public String toString() {
    return (isBreak ? "Break" : "Task") + "{" +
           "startTime=" + startTime +
           ", endTime=" + endTime +
           ", latitude=" + latitude +
           ", longitude=" + longitude +
           ", onDutyNonDrivingTime=" + onDutyNonDrivingTime.toMinutes() + "m" +
           '}';
}
