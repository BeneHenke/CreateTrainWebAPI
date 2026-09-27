package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Inventory and tank contents per carriage. Computed on demand (it walks every slot). */
public class CargoData {
    public UUID trainId;
    public List<Car> cars = new ArrayList<>();

    public static class Car {
        public int id;
        public int slots;
        public int usedSlots;
        /** Item id -> total count. */
        public Map<String, Integer> items = new LinkedHashMap<>();
        public List<Tank> tanks = new ArrayList<>();
    }

    public static class Tank {
        /** Fluid id, null for an empty tank. */
        public String fluid;
        /** Millibuckets. */
        public int amount;
        public int capacity;
    }

    public CargoData(Train train) {
        trainId = train.id;
        for (Carriage carriage : train.carriages) {
            Car car = new Car();
            car.id = carriage.id;
            MountedStorageManager storage = carriage.storage;
            if (storage != null) {
                IItemHandler items = storage.getAllItems();
                car.slots = items.getSlots();
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    ItemStack stack = items.getStackInSlot(slot);
                    if (stack.isEmpty())
                        continue;
                    car.usedSlots++;
                    car.items.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
                }
                IFluidHandler fluids = storage.getFluids();
                for (int tank = 0; tank < fluids.getTanks(); tank++) {
                    FluidStack fluid = fluids.getFluidInTank(tank);
                    Tank data = new Tank();
                    data.fluid = fluid.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
                    data.amount = fluid.getAmount();
                    data.capacity = fluids.getTankCapacity(tank);
                    car.tanks.add(data);
                }
            }
            cars.add(car);
        }
    }
}
