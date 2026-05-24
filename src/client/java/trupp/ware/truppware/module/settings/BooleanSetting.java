package trupp.ware.truppware.module.settings;

public class BooleanSetting extends Setting{


    public boolean enabled;


    public BooleanSetting(String name, boolean enabled) {
        super(name);
        this.enabled = enabled;
    }


    public void setEnabled(Boolean enabled){
        this.enabled = enabled;
    }

    public boolean isEnabled(){
        return this.enabled;
    }

    public boolean getValue(){
        return this.enabled;
    }


}
