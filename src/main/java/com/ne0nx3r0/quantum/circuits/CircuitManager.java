package com.ne0nx3r0.quantum.circuits;

import com.ne0nx3r0.quantum.ConfigConverter;
import com.ne0nx3r0.quantum.QuantumConnectors;
import com.ne0nx3r0.quantum.api.ICircuitManager;
import com.ne0nx3r0.quantum.api.Receiver;
import com.ne0nx3r0.quantum.receiver.DelayedReceiver;
import com.ne0nx3r0.quantum.receiver.ReceiverRegistry;
import com.ne0nx3r0.quantum.utils.MessageLogger;
import com.ne0nx3r0.quantum.utils.Normalizer;
import com.ne0nx3r0.quantum.utils.ValidMaterials;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Openable;
import org.bukkit.material.Redstone;

import java.io.File;
import java.util.*;

public final class CircuitManager implements ICircuitManager {

    // keepAlives - lamps/torches/etc that should stay powered regardless of redstone events
    public final static ArrayList<Block> keepAlives = new ArrayList<>();

    private MessageLogger messageLogger;
    // Temporary Holders for circuit creation
    private Map<String, Circuit> pendingCircuits;
    // Allow circuitTypes/circuits
    private QuantumConnectors plugin;
    // Lookup/Storage for circuits, and subsequently their receivers
    private Map<World, Map<Location, Circuit>> worlds = new HashMap<>();
    private CircuitLoader circuitLoader;

    // Main
    public CircuitManager(MessageLogger messageLogger, final QuantumConnectors qc) {
        this.messageLogger = messageLogger;
        this.plugin = qc;
        this.circuitLoader = new CircuitLoader(qc, worlds, this, messageLogger);

        //Create a holder for pending circuits
        this.pendingCircuits = new HashMap<>();

        //Convert circuits.yml to new structure
        if (new File(plugin.getDataFolder(), "circuits.yml").exists()) {
            new ConfigConverter(plugin, this.messageLogger).convertOldCircuitsYml();
        }

        //Init any loaded worlds
        for (World world : plugin.getServer().getWorlds()) {
            circuitLoader.loadWorld(world);
        }
    }

    public boolean isValidReceiver(Block block) {
        return ValidMaterials.validReceivers.contains(block.getType());
    }

    // Sender/Receiver_old checks
    public boolean isValidSender(Block block) {
        return ValidMaterials.validSenders.contains(block.getType());
    }

    public boolean shouldLeaveReceiverOn(Block block) {
        return keepAlives.contains(block);
    }

    public String getValidSendersString() {
        return getValidString(ValidMaterials.validSenders);
    }

    private String getValidString(List<Material> materials) {
        return String.join(", ", Normalizer.normalizeEnumNames(materials, Normalizer.NORMALIZER));
    }

    public String getValidReceiversString() {
        return getValidString(ValidMaterials.validReceivers);
    }

    // Circuit (sender) CRUD
    public void addCircuit(Location circuitLocation, Circuit newCircuit) {
        //Notably circuits are now created from a temporary copy, rather than piecemeal here.
        worlds.get(circuitLocation.getWorld()).put(circuitLocation, newCircuit);
    }

    public void addCircuit(Circuit pc) {
        worlds.get(pc.getLocation().getWorld())
                .put(pc.getLocation(), pc);
    }

    public Circuit getCircuit(Location circuitLocation) {
        return worlds.get(circuitLocation.getWorld()).get(circuitLocation);
    }

    public void removeCircuit(Location circuitLocation) {
        if (circuitExists(circuitLocation)) {
            worlds.get(circuitLocation.getWorld()).remove(circuitLocation);
        }
    }

    public boolean circuitExists(Location circuitLocation) {
        return worlds.get(circuitLocation.getWorld()).containsKey(circuitLocation);
    }

    // Circuit activation
    public void activateCircuit(Location lSender, int oldCurrent, int newCurrent) {
        activateCircuit(lSender, oldCurrent, newCurrent, 0);
    }

    public void activateCircuit(Location lSender, int oldCurrent, int newCurrent, int chain) {
        Circuit circuit = getCircuit(lSender);
        List<Receiver> receivers = new ArrayList<>(circuit.getReceivers());

        for (Receiver receiver : receivers) {


            if (isValidReceiver(receiver.getLocation().getBlock())) {

                int receiverOldCurrent = getBlockCurrent(receiver.getLocation().getBlock());

                switch (circuit.getCircuitType()) {

                    case OFF:
                        if (newCurrent == 0 && oldCurrent > 0) {
                            setReceiver(receiver, false);
                        }
                        break;
                    case ON:
                        if (newCurrent > 0 && oldCurrent == 0) {
                            setReceiver(receiver, true);
                        }
                        break;
                    case QUANTUM:
                        setReceiver(receiver, newCurrent > 0);
                        break;
                    case RANDOM:
                        if (newCurrent > 0 && oldCurrent == 0) {
                            setReceiver(receiver, new Random().nextBoolean());
                        }
                        break;

                    case REVERSE:
                        if (oldCurrent == 0 || newCurrent == 0) {
                            setReceiver(receiver, newCurrent <= 0);
                        }
                        break;

                    case TOGGLE:
                        if (newCurrent > 0 && oldCurrent == 0) {
                            setReceiver(receiver, getBlockCurrent(receiver.getLocation().getBlock()) <= 0);
                        }
                        break;
                }

                if (receiver.getLocation().getBlock().getType() == Material.TNT) { // TnT is one time use!
                    circuit.delReceiver(receiver);
                }

                if (chain <= QuantumConnectors.MAX_CHAIN_LINKS - 2 && circuitExists(receiver.getLocation())) {
                    if (QuantumConnectors.MAX_CHAIN_LINKS > 0) { //allow zero to be infinite
                        chain++;
                    }
                    activateCircuit(receiver.getLocation(), receiverOldCurrent, getBlockCurrent(receiver.getLocation().getBlock()), chain);
                }
            } else {
                circuit.delReceiver(receiver);
            }


        }
    }

    // TODO: 19.01.2017 remove
    public int getBlockCurrent(Block b) {
        Material material = b.getType();
        MaterialData md = b.getState().getData();
        if (md instanceof Redstone) {
            return ((Redstone) md).isPowered() ? 15 : 0;
        } else if (md instanceof Openable) {
            return ((Openable) md).isOpen() ? 15 : 0;
        } else if (ValidMaterials.LAMP.contains(material)) {
            return keepAlives.contains(b) ? 15 : 0;
        }

        return b.getBlockPower();
    }

    private void setReceiver(Receiver receiver, boolean on) {
        if (receiver.getDelay() > 0) {
            new DelayedReceiver(this.plugin, receiver).setActive(on);
        } else {
            receiver.setActive(on);
        }
    }

    // Temporary circuit stuff
// I really don't know what order this deserves among the existing class methods
    public Circuit addPendingCircuit(Player player, CircuitType type, int delay) {
        Circuit pc = new Circuit(player.getUniqueId(), type, delay);
        pendingCircuits.put(player.getName(), pc);
        return pc;
    }

    public Circuit getPendingCircuit(Player player) {
        return pendingCircuits.get(player.getName());
    }

    public boolean hasPendingCircuit(Player player) {
        return pendingCircuits.containsKey(player.getName());
    }

    public void removePendingCircuit(Player player) {
        pendingCircuits.remove(player.getName());
    }

    //Circuit Types
    public boolean isValidCircuitType(String type) {
        return CircuitType.getByName(type) != null;
    }

    public CircuitType getCircuitType(String sType) {
        return CircuitType.getByName(sType);
    }

    public CircuitLoader getCircuitLoader() {
        return circuitLoader;
    }

    public Set<Location> circuitLocations(World w) {
        return worlds.get(w).keySet();
    }

    //Receiver_old Types
    public Receiver fromType(Location location, long delay) {
        return ReceiverRegistry.fromType(location, delay);
    }


}
