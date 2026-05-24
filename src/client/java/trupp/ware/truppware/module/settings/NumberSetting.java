package trupp.ware.truppware.module.settings;

public class NumberSetting extends Setting{

    public double num;
    public double inc;
    public double max;
    public double min;

    public NumberSetting(String name, double min, double max, double defaultValue, double increment) {
        super(name);
        this.min = min;
        this.max = max;
        this.inc = increment;
        this.num = clamp(defaultValue);
    }

    private double clamp(double val) {
        return Math.max(min, Math.min(max, val));
    }


    public double getInc() {
        return inc;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public void increase(){
        setValue(num + inc);
    }

    public void decrease(){
        setValue(num - inc);
    }


    public void setValue(double value){
        this.num = value;
    }

    public double getNum() {
        return num;
    }

    public void setInc(double inc) {
        this.inc = inc;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setNum(double num) {
        this.num = num;
    }
}
