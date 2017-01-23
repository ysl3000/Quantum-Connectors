package com.ne0nx3r0.quantum.impl.interfaces;

import com.ne0nx3r0.quantum.api.receiver.ReceiverNotValidException;
import com.ne0nx3r0.quantum.api.receiver.ValueNotChangedException;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public interface Receiver extends ConfigurationSerializable {

    Location getLocation();

    /**
     * Will calculate real location for doubleBlocks
     */
    void calculateRealLocation();

    String getType();

    long getDelay();

    boolean isActive();

    void setActive(boolean powerOn) throws ValueNotChangedException, ReceiverNotValidException;

    boolean isValid();


}