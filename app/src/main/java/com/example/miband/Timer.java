package com.example.miband;

import java.util.TimerTask;

public class Timer {

    private int mili;
    private TimerTask task;
    private java.util.Timer timer;
    private Behavior behavior;

    public Timer(int time, Behavior b){
        mili = time;
        behavior = b;
        timer = new java.util.Timer();
    }

    public void start(){
    task = new TimerTask() {
        @Override
        public void run() {
            behavior.run();
        }
    };
    timer.scheduleAtFixedRate(task,0,mili);
}

    public void stop(){
        timer.cancel();
        task.cancel();
    }
}

