package trupp.ware.event;

import trupp.ware.event.events.Timing;

public class Event<t> {


    public boolean isPre;
    public boolean isPost;
    public String name;
    public Timing time;

    public boolean canceled;


    public Event(String name){
        this.name = name;
    }

    public void setTime(Timing time){
        this.time = time;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public Timing getTime(){
        return time;
    }


}
