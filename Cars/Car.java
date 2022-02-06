package Cars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Car {
    private static Car instance = null;
    private final int index;
    private final double v_max;
    private final double accelerate;
    private final Random random;

    private int position;
    private double v_actual;
    private boolean endOfRide;

    private final int size = 100;
    private final int startPos = -size - 50;
    private final int startDistance = 120;

    Car(ArrayList<Car> cars) {
        if (cars.isEmpty()){
            this.position = startPos;
        } else {
            Car lastCar = cars.get(cars.size() - 1);
            this.position = lastCar.getPosition() - (lastCar.getSize() / 2) - startDistance;
        }
        this.index = cars.size();

        this.v_actual = 1.0;
        this.random = new Random();
        this.endOfRide = false;

        //this.v_max = random.nextInt(11) + 10;
        this.v_max = 15;
        List<Double> accelerates = Arrays.asList(0.5, 1.0, 1.5, 2.0, 3.0);
        this.accelerate = accelerates.get(random.nextInt(accelerates.size()));
    }

    public void updatePosition(ArrayList<Car> cars, int i){
        if (index == i && i != 0){

            if (v_actual + accelerate <= v_max){
                int rand = random.nextInt(4);
                if (rand < 2)
                    v_actual += accelerate;
                else if (rand == 2){
                    v_actual -= accelerate/2;
                    if (v_actual < 1.0) v_actual = 1.0;
                }
            }
            if (position + v_actual + startDistance < cars.get(i - 1).getPosition()){
                position += v_actual;
            } else v_actual = cars.get(i - 1).getV();

        } else {
            if (v_actual + accelerate <= v_max){
                v_actual += accelerate;
            }
            position += v_actual;
        }

        if(position >= 1200){
            endOfRide = true;
        }
    }

    public void setPosition(int pos){
        this.position = pos;
    }

    public boolean isEndOfRide(){
        if(endOfRide){
            endOfRide = false;
            return true;
        }
        return false;
    }

    public int getStartDistance(){
        return startDistance;
    }
    public int getPosition() { return position; }
    public int getSize() { return size; }
    public double getV(){
        return v_actual;
    }
    public int getStartPos(){
        return startPos;
    }

    static public Car getInstance()
    {
        if(instance==null) instance = new Car(new ArrayList<Car>());
        return instance;
    }
}
