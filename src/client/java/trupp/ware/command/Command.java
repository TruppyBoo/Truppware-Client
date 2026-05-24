package trupp.ware.command;

public class Command {



    public String name;

    public Command(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void execute(String args[]){

    }

    protected boolean isArg(String[] args, int index, String match) {
        return args.length > index && args[index].equalsIgnoreCase(match);
    }


}
