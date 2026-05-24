package trupp.ware.truppware.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.settings.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Module{

    public int key;
    public boolean toggled;
    public Category category;
    public String description;
    public List<Setting> settings = new ArrayList<>();
    public String name;


    public void addSettings(Setting... setting){
        settings.addAll(Arrays.asList(setting));

    }

    public List<Setting> getSettings() {
        return settings;
    }


    public Module(String name, Category category, String description, int key){
        this.key = key;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public void setKey(int key){
        this.key = key;
    }

    public void setToggled(boolean toggled){
        this.toggled = toggled;
    }

    public void setCategory(Category category){
        this.category = category;
    }

    public void getDescription(String desc){
        this.description = desc;
    }


    public void toggle(){
        this.toggled = !this.toggled;
        if(toggled){
            onEnable();
        }else{
            OnDisable();
        }
        trupp.ware.truppware.module.render.Modulelist.pushNotification(getName(), toggled);
    }


    public void onEnable(){

    }

    public void OnDisable(){

    }

    public void onEvent(Event e, Timing time){
        TruppWareClient.trupp.logger.info(e.name + e.time);
    }


    public int getKey(){
        return key;
    }

    public boolean getToggled(){
        return toggled;
    }

    public Category getcategory(){
        return category;
    }

    public String getDescription(){
        return description;
    }

    public String getName(){
        return name;
    }






}
