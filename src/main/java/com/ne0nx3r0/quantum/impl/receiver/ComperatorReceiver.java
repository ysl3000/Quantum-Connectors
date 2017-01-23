package com.ne0nx3r0.quantum.impl.receiver;

import com.ne0nx3r0.quantum.api.receiver.AbstractKeepAliveReceiver;
import com.ne0nx3r0.quantum.api.receiver.ReceiverNotValidException;
import com.ne0nx3r0.quantum.api.receiver.ValueNotChangedException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.material.Comparator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Yannick on 22.01.2017.
 */
public class ComperatorReceiver extends AbstractKeepAliveReceiver {

    /**
     * only use to getValidMaterials
     */
    public ComperatorReceiver() {
        super();
    }

    public ComperatorReceiver(Location location) {
        super(location);
    }

    public ComperatorReceiver(Location location, Integer delay) {
        super(location, delay);
    }

    public ComperatorReceiver(Map<String, Object> map) {
        super(map);
    }

    @Override
    public boolean isActive() {
        if (!isValid()) return false;
        BlockState blockState = location.getBlock().getState();
        Comparator comparator = (Comparator) blockState.getData();
        return comparator.isPowered();
    }

    @Override
    public void setActive(boolean powerOn) {
        try {
            super.setActive(powerOn);
        } catch (ValueNotChangedException | ReceiverNotValidException e) {
            return;
        }

        BlockState blockState = location.getBlock().getState();
        Comparator comparator = (Comparator) blockState.getData();
        comparator = new Comparator(comparator.getFacing(), comparator.isSubtractionMode(), powerOn);
        blockState.setData(comparator);
        blockState.update();

    }

    @Override
    public List<Material> getValidMaterials() {
        return Arrays.asList(Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON);
    }
}