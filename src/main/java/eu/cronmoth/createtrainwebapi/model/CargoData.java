package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.registries.BuiltInRegistries;

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

    /** Fabric counts fluids in droplets (81000 per bucket); the API reports millibuckets like on Forge. */
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    public CargoData(Train train) {
        trainId = train.id;
        for (Carriage carriage : train.carriages) {
            Car car = new Car();
            car.id = carriage.id;
            MountedStorageManager storage = carriage.storage;
            if (storage != null) {
                SlottedStorage<ItemVariant> items = storage.getAllItems();
                car.slots = items.getSlotCount();
                for (int slot = 0; slot < items.getSlotCount(); slot++) {
                    SingleSlotStorage<ItemVariant> stack = items.getSlot(slot);
                    if (stack.isResourceBlank() || stack.getAmount() == 0)
                        continue;
                    car.usedSlots++;
                    car.items.merge(BuiltInRegistries.ITEM.getKey(stack.getResource().getItem()).toString(),
                            (int) stack.getAmount(), Integer::sum);
                }
                Storage<FluidVariant> fluids = storage.getFluids();
                for (StorageView<FluidVariant> tank : fluids) {
                    Tank data = new Tank();
                    data.fluid = tank.isResourceBlank() ? null : BuiltInRegistries.FLUID.getKey(tank.getResource().getFluid()).toString();
                    data.amount = (int) (tank.getAmount() / DROPLETS_PER_MB);
                    data.capacity = (int) (tank.getCapacity() / DROPLETS_PER_MB);
                    car.tanks.add(data);
                }
            }
            cars.add(car);
        }
    }
}
