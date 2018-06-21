package com.bi.dashboard.services;


import java.time.LocalTime;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class RefreshService extends Service {     
    private int waitTime;

    @Override
    protected Task createTask() {
        return new Task() {
            @Override
            protected Void call() throws InterruptedException {
                while (true) {
                    if (isCancelled()) {
                        System.out.println("Service Stopped");
                        break;
                    }
                    Thread.sleep(waitTime);
                    this.updateMessage(LocalTime.now().toString());
                }
                return null;
            }
        };
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
    public int getWaitTime(){
        return this.waitTime;
    }
}
