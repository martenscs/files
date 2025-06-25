import java.time.Duration;
import java.time.LocalDateTime;

class Power {
    private Duration accumulatedDriveTime;
    private Duration accumulatedOnDutyTime;
    private Duration accumulatedWeeklyOnDutyTime;
    private LocalDateTime dutyStartTime;

    public Power(Duration accumulatedDriveTime, Duration accumulatedOnDutyTime, Duration accumulatedWeeklyOnDutyTime, LocalDateTime dutyStartTime) {
        if (accumulatedDriveTime.isNegative() || accumulatedOnDutyTime.isNegative() || accumulatedWeeklyOnDutyTime.isNegative() || dutyStartTime == null) {
            throw new IllegalArgumentException("Invalid HOS parameters");
        }
        this.accumulatedDriveTime = accumulatedDriveTime;
        this.accumulatedOnDutyTime = accumulatedOnDutyTime;
        this.accumulatedWeeklyOnDutyTime = accumulatedWeeklyOnDutyTime;
        this.dutyStartTime = dutyStartTime;
    }

    public Duration getAccumulatedDriveTime() {
        return accumulatedDriveTime;
    }

    public void addDriveTime(Duration driveTime) {
        this.accumulatedDriveTime = accumulatedDriveTime.plus(driveTime);
    }

    public Duration getAccumulatedOnDutyTime() {
        return accumulatedOnDutyTime;
    }

    public void addOnDutyTime(Duration onDutyTime) {
        this.accumulatedOnDutyTime = accumulatedOnDutyTime.plus(onDutyTime);
        this.accumulatedWeeklyOnDutyTime = accumulatedWeeklyOnDutyTime.plus(onDutyTime);
    }

    public Duration getAccumulatedWeeklyOnDutyTime() {
        return accumulatedWeeklyOnDutyTime;
    }

    public LocalDateTime getDutyStartTime() {
        return dutyStartTime;
    }

    public void apply34HourRestart() {
        this.accumulatedWeeklyOnDutyTime = Duration.ZERO;
        this.accumulatedDriveTime = Duration.ZERO;
        this.accumulatedOnDutyTime = Duration.ZERO;
        this.dutyStartTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Power{" +
               "accumulatedDriveTime=" + accumulatedDriveTime.toHours() + "h" + accumulatedDriveTime.toMinutesPart() + "m" +
               ", accumulatedOnDutyTime=" + accumulatedOnDutyTime.toHours() + "h" + accumulatedOnDutyTime.toMinutesPart() + "m" +
               ", accumulatedWeeklyOnDutyTime=" + accumulatedWeeklyOnDutyTime.toHours() + "h" + accumulatedWeeklyOnDutyTime.toMinutesPart() + "m" +
               ", dutyStartTime=" + dutyStartTime +
               '}';
    }
}
