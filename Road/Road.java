package Road;

public class Road {
    private static Road instance = null;
    private final int prepareTime;
    private int timer;
    private boolean isPreparing;

    private Road() {
        this.timer = 0;
        this.isPreparing = false;
        this.prepareTime = 20;
    }

    public void updateTime(){
        timer++;
        if (timer >= prepareTime){
            isPreparing = false;
            timer = 0;
        }
    }

    public void setPrepare(){
        isPreparing = true;
    }
    public boolean isPrepareNow(){
        return isPreparing;
    }

    static public Road getInstance()
    {
        if(instance==null) instance = new Road();
        return instance;
    }
}
