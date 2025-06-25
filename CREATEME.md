Below is the content for a CreateMe.md document that provides detailed instructions on how to recreate the Java project for managing a driving schedule with U.S. Hours of Service (HOS) compliance, including the TimeSpanLocation, Power, and DrivingSchedule classes, as well as support for on-duty non-driving time and weekly HOS limits. The document assumes familiarity with Java and a basic development environment.CreateMe.mdThis document provides step-by-step instructions to recreate a Java project for managing a driving schedule with compliance to U.S. Federal Motor Carrier Safety Administration (FMCSA) Hours of Service (HOS) regulations. The project includes classes to track tasks, driver HOS status, and automatic scheduling adjustments for breaks, drive times, and weekly limits.Project OverviewThe project consists of three main Java classes:TimeSpanLocation: Represents a task or break with start/end times, location (latitude/longitude), on-duty non-driving time, and a flag to indicate breaks.Power: Tracks a driver’s HOS status, including accumulated daily driving time, daily on-duty time, weekly on-duty time, and duty start time.DrivingSchedule: Manages a list of tasks, automatically adjusts task times based on drive time between locations, inserts mandatory 30-minute breaks, and enforces daily (11-hour driving, 14-hour duty) and weekly (70-hour on-duty) HOS limits.FeaturesHOS Compliance:30-minute break after 8 hours of driving.11-hour daily driving limit.14-hour daily duty limit (including on-duty non-driving time).70-hour weekly on-duty limit (over 8 days).Drive Time Calculation: Uses the Haversine formula to estimate drive time between locations based on an average speed of 50 km/h.Automatic Scheduling: Adjusts task times and inserts breaks when tasks are added, respecting existing HOS data.On-Duty Non-Driving Time: Supports tasks with additional on-duty time (e.g., loading/unloading).Error Handling: Validates against HOS limits and throws exceptions for violations.PrerequisitesJava Development Kit (JDK): Version 8 or higher (uses java.time API).Integrated Development Environment (IDE): Recommended (e.g., IntelliJ IDEA, Eclipse, or VS Code with Java extensions).Build Tool (Optional): Maven or Gradle for dependency management (not required for this project as it uses only standard Java libraries).Text Editor: For creating and editing Java files.Command Line or Terminal: For compiling and running the Java program.Project SetupStep 1: Set Up the Project DirectoryCreate a new directory for the project, e.g., DrivingScheduleProject.Inside the directory, create a src folder to hold the Java source files.mkdir DrivingScheduleProject
cd DrivingScheduleProject
mkdir srcStep 2: Create the Java ClassesCreate the following Java files in the src directory. No package declaration is used for simplicity, but you can add one (e.g., package com.driving.schedule;) if organizing in a package structure.File 1: TimeSpanLocation.javaThis class represents a task or break with time, location, and on-duty non-driving time.import java.time.Duration;
import java.time.LocalDateTime;

public class TimeSpanLocation {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double latitude;
    private double longitude;
    private boolean isBreak;
    private Duration onDutyNonDrivingTime;

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
}File 2: Power.javaThis class tracks the driver’s HOS status, including daily and weekly limits.import java.time.Duration;
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
}File 3: DrivingSchedule.javaThis class manages the schedule, enforces HOS rules, and adjusts task times.import java.time.Duration;
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
}File 4: Main.javaThis class demonstrates usage of the scheduling system.import java.time.Duration;
import java.time.LocalDateTime;

class Main {
    public static void main(String[] args) {
        Power driverPower = new Power(
            Duration.ofHours(6),
            Duration.ofHours(8),
            Duration.ofHours(50),
            LocalDateTime.of(2025, 6, 25, 5, 0)
        );

        DrivingSchedule schedule = new DrivingSchedule(driverPower);

        TimeSpanLocation task1 = new TimeSpanLocation(
            LocalDateTime.of(2025, 6, 25, 11, 0),
            LocalDateTime.of(2025, 6, 25, 13, 0),
            40.7128,
            -74.0060,
            false,
            Duration.ofMinutes(30)
        );

        TimeSpanLocation task2 = new TimeSpanLocation(
            LocalDateTime.of(2025, 6, 25, 13, 30),
            LocalDateTime.of(2025, 6, 25, 14, 30),
            40.7357,
            -74.1724,
            false,
            Duration.ofMinutes(15)
        );

        schedule.addTask(task1);
        schedule.printSchedule();

        System.out.println("\nInserting new task...");
        schedule.addTask(task2);
        schedule.printSchedule();
    }
}Step 3: Compile and Run the ProjectNavigate to the src directory:cd DrivingScheduleProject/srcCompile the Java files:javac *.javaRun the program:java MainStep 4: Expected OutputThe program will output the initial schedule, insert a new task, and adjust times with a break, respecting HOS limits. Example output (assuming ~15 km between tasks, ~18-minute drive time):Driver HOS: Power{accumulatedDriveT
