package trupp.ware.truppware.module.settings;

import java.util.List;

public class ModeSetting extends Setting{


    public List<String> modes;
    public String currentMode;
    public int index;

    public ModeSetting(String name, List<String> modes) {
        super(name);
        this.modes = modes;
        this.index = 0;
        this.currentMode = modes.get(0);
    }


    public void cycle(){
        index++;
        if(index >= modes.size()){
            index = 0;
        }
        currentMode = modes.get(index);
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public List<String> getModes() {
        return modes;
    }

    public void setModes(List<String> modes) {
        this.modes = modes;
    }

}
