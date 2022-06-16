package com.timevale.drc.base;

public class TaskExample implements Task {

    @Override
    public void start() {
        System.out.println("start");
    }

    @Override
    public void stop(String cause) {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

}
