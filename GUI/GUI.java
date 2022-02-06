package GUI;

import javax.swing.*;
import java.util.ArrayList;

public class GUI {
    public JFrame window;
    public Panel panel = new Panel();

    private final ArrayList<Integer> times;
    private int time;
    private float avg;
    private static GUI instance = null;
    private boolean isWindow = false;

    private GUI() {
        this.times = new ArrayList<>();
        this.avg = 0.0f;
        this.time = 0;
    }

    public void addPanel(){
        if(!isWindow){
            window = new JFrame("CarsOnRoad");
            window.setContentPane(panel);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.pack();
            window.setVisible(true);
            isWindow = true;
        }
    }

    public void addRide(){
        if (times.isEmpty()) times.add(time + 1);
        else {
            int fullTime = 0;
            for (int i : times) fullTime += i;
            times.add(time + 1 - fullTime);
        }

        int sum = 0;
        for(int i : times)
            sum += i;
        avg = (float)(sum / times.size());
    }

    public void updateTime(){
        time++;
    }
    public ArrayList<Integer> getTimes() { return times; }
    public int getRides() {
        return times.size();
    }
    public int getTime() {
        return time;
    }
    public float getAvg() {
        return avg;
    }

    static public GUI getInstance()
    {
        if(instance==null) instance = new GUI();
        return instance;
    }
}
