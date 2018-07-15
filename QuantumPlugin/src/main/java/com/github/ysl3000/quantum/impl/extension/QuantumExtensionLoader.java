package com.github.ysl3000.quantum.impl.extension;

import com.github.ysl3000.quantum.api.IQuantumConnectorsAPI;
import com.github.ysl3000.quantum.api.QuantumExtension;
import com.github.ysl3000.quantum.impl.utils.MessageLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.zip.ZipFile;

/**
 * Created by Yannick on 23.01.2017.
 */
public class QuantumExtensionLoader {

    private final IQuantumConnectorsAPI api;
    private final MessageLogger messageLogger;
    private final Set<QuantumExtension> quantumExtensions = new HashSet<>();
    private final Set<Class<?>> quantumExtensionsClass = new HashSet<>();
    private final File file;

    public QuantumExtensionLoader(IQuantumConnectorsAPI api, MessageLogger messageLogger, File file) {
        this.api = api;
        this.messageLogger = messageLogger;
        this.file = file;
    }


    public void load() {

        if (file.exists() && file.isDirectory()) {

            File[] files = file.listFiles((dir, name) -> name.endsWith(".jar"));

            for (File jar : files) {
                String mainClass = null;
                try {
                    ZipFile zipFile = new ZipFile(jar);

                    InputStream is = zipFile.getInputStream(zipFile.getEntry("extension.yml"));

                    YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
                    mainClass = config.getString("main");

                    ClassLoader l = URLClassLoader.newInstance(new URL[]{jar.toURI().toURL()}, getClass().getClassLoader());

                    Class<?> clazz = l.loadClass(mainClass);
                    quantumExtensionsClass.add(clazz);

                } catch (IOException e) {
                    messageLogger.error("Error while loading module file " + jar.getName());
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    messageLogger.error("Class not found! Wrong main defined in extension.yml?: " + jar.getName() + " class: " + mainClass);
                    e.printStackTrace();
                }


            }


        }
    }

    @FunctionalInterface
    public static interface CallBack{
        void call();
    }



    public void enable(CallBack onFinish) {
        for (Class<?> clazz : quantumExtensionsClass) {

            try {

                if (QuantumExtension.class.isAssignableFrom(clazz)) {
                    Constructor<? extends QuantumExtension> quantumExtensionConstructor = clazz.asSubclass(QuantumExtension.class).getConstructor();

                    QuantumExtension quantumExtension = quantumExtensionConstructor.newInstance();
                    quantumExtension.onEnable(api);
                    quantumExtensions.add(quantumExtension);
                    messageLogger.log(Level.INFO, quantumExtension.getExtensionName() + " enabled!");

                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        onFinish.call();
    }

    public void disable() {
        for (QuantumExtension extension : quantumExtensions) {
            extension.onDisable();
            messageLogger.log(Level.INFO, extension.getExtensionName() + " disabled!");
        }
    }


}
