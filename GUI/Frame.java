package GUI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Frame {
    public ArrayList<Integer> carsPosition;
    public boolean needUpdate;

    private final BufferedImage car;
    private final BufferedImage road;

    public Frame() throws IOException {
        this.carsPosition = new ArrayList<>();
        this.needUpdate = true;
        car = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Images/car.png")));
        road = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Images/road.png")));
    }

    public void update(){
        needUpdate = false;
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(255, 255,255));
        g.fillRect(0, 0, Panel.WIDTH, Panel.HEIGHT);
        g.setFont(new Font("TimesRoman", Font.PLAIN, 30));

        g.drawImage(road, 0, 0,1200, 100,null);

        if (!carsPosition.isEmpty())
            for (int i : carsPosition){
                g.drawImage(car, i, 0, 100, 50, null);
                g.drawString(String.valueOf(carsPosition.indexOf(i) + 1), i + 35, 35);
            }

        if (GUI.getInstance().getTimes().size() > 0){
            g.setColor(new Color(134, 134, 134));
            int fullTime = 0;
            for (int i : GUI.getInstance().getTimes()) fullTime += i;
            g.fillRect(0, 50, (int)(1200 * ((GUI.getInstance().getTime() + 1)
                    - fullTime ) / GUI.getInstance().getAvg()), 50);
        }

        g.setColor(new Color(255, 255,255));
        String numberString = "Number of cars: " + carsPosition.size();
        g.drawString(numberString,50, 88);
        String avgString = "Time: " + GUI.getInstance().getTime() + ", Avg time: " + GUI.getInstance().getAvg();
        g.drawString(avgString,750, 88);
    }
}