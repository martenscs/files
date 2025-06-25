import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class DrivingSchedule {
    private List<TimeSpanLocation> tasks;
    private Power driverPower;
    private static final double AVERAGE_SPEED_KM_PER_HOUR = 50.0;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final Duration BREAK_DURATION = Duration.ofMinutes(30);
    private static final Duration MAX_DRIVING_WITHOUT_BREAK = Duration.ofHours(8);
    private static final Duration MAX_DRIVING_LIMIT = Duration.ofHours(11);
    private static final Duration MAX_DUTY_LIMIT = Duration.ofHours(14);
    private static final Duration MAX_WEEKLY_ON_DUTY_LIMIT = Duration.ofHours(70);

    public DrivingSchedule(Power driverPower) {
        this.tasks = new ArrayList<>();
        this.driverPower = driverPower;
    }

    private double calculateDistance(TimeSpanLocation task1, TimeSpanLocation task2) {
        double lat1 = Math.toRadians(task1.getLatitude());
        double lon1 = Math.toRadians(task1.getLongitude());
        double lat2 = Math.toRadians(task2.getLatitude());
        double lon2 = Math.toRadians(task2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private Duration calculateDriveTime(TimeSpanLocation task1, TimeSpanLocation task2) {
        double distanceKm = calculateDistance(task1, task2);
        double hours = distanceKm / AVERAGE_SPEED_KM_PER_HOUR;
        long minutes = Math.round(hours * 60);
        return Duration.ofMinutes(minutes);
    }

    private Duration calculateCumulativeDrivingTime(int endIndex) {
        Duration totalDriving = driverPower.getAccumulatedDriveTime();
        for (int i = 0; i <= endIndex && i < tasks.size(); i++) {
            TimeSpanLocation task = tasks.get(i);
            if (!task.isBreak()) {
                totalDriving = totalDriving.plus(task.getDrivingDuration());
            }
        }
        return totalDriving;
    }

    private Duration calculateCumulativeOnDutyTime(int endIndex) {
        Duration totalOnDuty = driverPower.getAccumulatedOnDutyTime();
        for (int i = 0; i <= endIndex && i < tasks.size(); i++) {
            TimeSpanLocation task = tasks.get(i);
            if (!task.isBreak()) {
                totalOnDuty = totalOnDuty.plus(task.getTotalOnDutyDuration());
            }
        }
        return totalOnDuty;
    }

    private Duration calculateCumulativeWeeklyOnDutyTime(int endIndex) {
        Duration totalWeeklyOnDuty = driverPower.getAccumulatedWeeklyOnDutyTime();
        for (int i = 0; i <= endIndex && i < tasks.size(); i++) {
            TimeSpanLocation task = tasks.get(i);
            if (!task.isBreak()) {
                totalWeeklyOnDuty = totalWeeklyOnDuty.plus(task.getTotalOnDutyDuration());
            }
        }
        return totalWeeklyOnDuty;
    }

    private boolean needsBreakBefore(int index) {
        if (index == 0 || tasks.isEmpty()) {
            return driverPower.getAccumulatedDriveTime().compareTo(MAX_DRIVING_WITHOUT_BREAK) >= 0;
        }

        LocalDateTime lastBreakEnd = driverPower.getDutyStartTime();
        int lastBreakIndex = -1;
        for (int i = index - 1; i >= 0; i--) {
            if (tasks.get(i).isBreak()) {
                lastBreakEnd = tasks.get(i).getEndTime();
                lastBreakIndex = i;
                break;
            }
        }

        Duration drivingSinceLastBreak = Duration.ZERO;
        for (int i = lastBreakIndex + 1; i < index; i++) {
            if (!tasks.get(i).isBreak()) {
                drivingSinceLastBreak = drivingSinceLastBreak.plus(tasks.get(i).getDrivingDuration());
            }
        }
        if (lastBreakIndex == -1) {
            drivingSinceLastBreak = drivingSinceLastBreak.plus(driverPower.getAccumulatedDriveTime());
        }

        return drivingSinceLastBreak.compareTo(MAX_DRIVING_WITHOUT_BREAK) >= 0;
    }

    private void insertBreak(int index, LocalDateTime startTime, double latitude, double longitude) {
        TimeSpanLocation breakTask = new TimeSpanLocation(
            startTime,
            startTime.plus(BREAK_DURATION),
            latitude,
            longitude,
            true,
            Duration.ZERO
        );
        tasks.add(index, breakTask);
    }

    public void insertTask(int index, TimeSpanLocation newTask) {
        if (newTask == null || index < 0 || index > tasks.size()) {
            throw new IllegalArgumentException("Invalid task or index");
        }
        if (newTask.isBreak()) {
            throw new IllegalArgumentException("Cannot insert a break as a task");
        }

        Duration newDriveTime = newTask.getDrivingDuration();
        Duration newOnDutyTime = newTask.getTotalOnDutyDuration();
        Duration totalDriveTime = calculateCumulativeDrivingTime(index - 1).plus(newDriveTime);
        if (totalDriveTime.compareTo(MAX_DRIVING_LIMIT) > 0) {
            throw new IllegalStateException("New task exceeds 11-hour driving limit");
        }

        Duration totalWeeklyOnDutyTime = calculateCumulativeWeeklyOnDutyTime(index - 1).plus(newOnDutyTime);
        if (totalWeeklyOnDutyTime.compareTo(MAX_WEEKLY_ON_DUTY_LIMIT) > 0) {
            throw new IllegalStateException("New task exceeds 70-hour weekly on-duty limit");
        }

        LocalDateTime dutyEndTime = newTask.getEndTime().plus(newTask.getOnDutyNonDrivingTime());
        if (index > 0) {
            TimeSpanLocation prevTask = tasks.get(index - 1);
            Duration driveTime = calculateDriveTime(prevTask, newTask);
            dutyEndTime = prevTask.getEndTime().plus(driveTime).plus(newDriveTime).plus(newTask.getOnDutyNonDrivingTime());
        }
        Duration dutyTime = Duration.between(driverPower.getDutyStartTime(), dutyEndTime);
        if (dutyTime.compareTo(MAX_DUTY_LIMIT) > 0) {
            throw new IllegalStateException("New task exceeds 14-hour duty limit");
        }

        tasks.add(index, newTask);

        if (needsBreakBefore(index)) {
            TimeSpanLocation prevTask = index > 0 ? tasks.get(index - 1) : null;
            LocalDateTime breakStartTime = prevTask != null ? prevTask.getEndTime() : newTask.getStartTime();
            double breakLat = prevTask != null ? prevTask.getLatitude() : newTask.getLatitude();
            double breakLon = prevTask != null ? prevTask.getLongitude() : newTask.getLongitude();
            insertBreak(index, breakStartTime, breakLat, breakLon);
            index++;
        }

        adjustTaskTimes(index);

        driverPower.addDriveTime(newDriveTime);
        driverPower.addOnDutyTime(newOnDutyTime);
    }

    public void addTask(TimeSpanLocation task) {
        if (task != null) {
            insertTask(tasks.size(), task);
        }
    }

    private void adjustTaskTimes(int startIndex) {
        if (tasks.isEmpty() || startIndex >= tasks.size()) {
            return;
        }

        for (int i = startIndex; i < tasks.size(); i++) {
            TimeSpanLocation currentTask = tasks.get(i);
            if (i == 0) {
                Duration dutyTime = Duration.between(driverPower.getDutyStartTime(), currentTask.getEndTime().plus(currentTask.getOnDutyNonDrivingTime()));
                if (dutyTime.compareTo(MAX_DUTY_LIMIT) > 0) {
                    throw new IllegalStateException("Task at index " + i + " exceeds 14-hour duty limit");
                }
                continue;
            }

            TimeSpanLocation prevTask = tasks.get(i - 1);
            Duration driveTime = currentTask.isBreak() ? Duration.ZERO : calculateDriveTime(prevTask, currentTask);
            LocalDateTime expectedStartTime = prevTask.getEndTime().plus(driveTime);

            if (!currentTask.isBreak()) {
                Duration totalDriveTime = calculateCumulativeDrivingTime(i);
                if (totalDriveTime.compareTo(MAX_DRIVING_LIMIT) > 0) {
                    throw new IllegalStateException("Task at index " + i + " exceeds 11-hour driving limit");
                }
                Duration totalWeeklyOnDutyTime = calculateCumulativeWeeklyOnDutyTime(i);
                if (totalWeeklyOnDutyTime.compareTo(MAX_WEEKLY_ON_DUTY_LIMIT) > 0) {
                    throw new IllegalStateException("Task at index " + i + " exceeds 70-hour weekly on-duty limit");
                }
                Duration dutyTime = Duration.between(driverPower.getDutyStartTime(), expectedStartTime.plus(currentTask.getTotalOnDutyDuration()));
                if (dutyTime.compareTo(MAX_DUTY_LIMIT) > 0) {
                    throw new IllegalStateException("Task at index " + i + " exceeds 14-hour duty limit");
                }
            }

            Duration shiftAmount = Duration.between(currentTask.getStartTime(), expectedStartTime);
            currentTask.shiftBy(shiftAmount);

            if (!currentTask.isBreak() && needsBreakBefore(i + 1) && i + 1 < tasks.size()) {
                insertBreak(i + 1, currentTask.getEndTime(), currentTask.getLatitude(), currentTask.getLongitude());
            }
        }
    }

    public List<TimeSpanLocation> getTasks() {
        return new ArrayList<>(tasks);
    }

    public Power getDriverPower() {
        return driverPower;
    }

    public void printSchedule() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks in the schedule.");
            return;
        }
        System.out.println("Driver HOS: " + driverPower);
        System.out.println("Driving Schedule:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((tasks.get(i).isBreak() ? "Break " : "Task ") + (i + 1) + ": " + tasks.get(i));
        }
    }

    public int getTaskCount() {
        return tasks.size();
    }
}
