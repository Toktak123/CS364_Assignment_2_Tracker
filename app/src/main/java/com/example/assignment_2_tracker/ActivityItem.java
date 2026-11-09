package com.example.assignment_2_tracker;

public class ActivityItem {
    public int id;
    public long startTime;
    public int durationSec;
    public int steps;
    public int shakes;
    public boolean isStepRecord;
    public boolean isShakeRecord;

    public void setStepRecord(boolean b) {
        isStepRecord = b;
    }

    public void setShakeRecord(boolean b) {
        isShakeRecord = b;
    }

    public boolean getStepRecord() {
        return isStepRecord;
    }
    public boolean getShakeRecord() {
        return isShakeRecord;
    }


    public ActivityItem(int id, long startTime, int durationSec, int steps, int shakes) {
        this.id = id;
        this.startTime = startTime;
        this.durationSec = durationSec;
        this.steps = steps;
        this.shakes = shakes;
    }

    public int getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public int getSteps() {
        return steps;
    }

    public int getShakes() {
        return shakes;
    }

    public double getStepsPerMin() {
        return steps * 60.0 / Math.max(durationSec, 1);
    }

    public double getShakesPerMin() {
        return shakes * 60.0 / Math.max(durationSec, 1);
    }
}
