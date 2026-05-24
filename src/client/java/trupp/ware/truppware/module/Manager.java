package trupp.ware.truppware.module;

import trupp.ware.TruppWareClient;
import trupp.ware.config.ConfigManager;
import trupp.ware.truppware.module.COMBAT.*;
import trupp.ware.truppware.module.exploit.PingSpoof;
import trupp.ware.truppware.module.modules.MOVEMENT.Bhop;
import trupp.ware.truppware.module.modules.MOVEMENT.Fly;
import trupp.ware.truppware.module.modules.MOVEMENT.Noslow;
import trupp.ware.truppware.module.modules.MOVEMENT.Sprint;
import trupp.ware.truppware.module.player.*;
import trupp.ware.truppware.module.render.*;

import java.util.ArrayList;
import java.util.List;

public class Manager {


    public List<Module> modules = new ArrayList<Module>();
    public static Manager trupp = new Manager();


    public void InitializeModules() {
        TruppWareClient.trupp.logger.info("Loading Modules");
        modules.add(new Fly());
        modules.add(new Sprint());
        modules.add(new Modulelist());
        modules.add(new Autoclicker());
        modules.add(new ESP());
        modules.add(new HUD());
        modules.add(new Velocity());
        modules.add(new WTap());
        modules.add(new AimAssist());
        modules.add(new Scaffold());
        modules.add(new Aura());
        modules.add(new RealAutoClicker());
        modules.add(new BlockHit());
        modules.add(new FastPlace());
        modules.add(new CrystalAutoClicker());
        modules.add(new EdgeDetect());
        modules.add(new ShieldBreaker());
        modules.add(new AutoMace());
        modules.add(new AutoTotem());
        modules.add(new AutoAnchor());
        modules.add(new SpearSpam());
        modules.add(new FakeLag());
        modules.add(new Backtrack());
        modules.add(new Reach());
        modules.add(new Bhop());
        modules.add(new Fullbright());
        modules.add(new Animation());
        modules.add(new ChestStealer());
        modules.add(new AutoArmour());
        modules.add(new SpearFly());
        modules.add(new TargetHud());
   //     modules.add(new PingSpoof());
        modules.add(new Noslow());
        TruppWareClient.trupp.logger.info("Modules Loaded");
    }


    public void toggle(int key, int action) {
        for (Module m : modules) {
            if (m.getKey() == key && action == 1) {
                m.toggle();
            }
        }
    }


    public List<Module> getModuleByCat(Category category) {
        List<Module> moduleList = new ArrayList<Module>();

        for (Module m : modules) {
            if (m.category == category) {
                moduleList.add(m);
            }
        }

        return moduleList;


    }

    public Module getModuleByName(String modname) {


        for (Module m : modules) {

            if (m.name == modname) {
                return m;
            }
        }


        return null;
    }


}
