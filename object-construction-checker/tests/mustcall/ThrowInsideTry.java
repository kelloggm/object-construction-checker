// Based on an FP in ZK.

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.nio.file.Files;

class ThrowInsideTry {

    public static class ConfigException extends Exception {

        public ConfigException(String s) {
            super(s);
        }

        public ConfigException(String s, Throwable t) {
            super(s, t);
        }
    }

    String configFileStr, dynamicConfigFileStr, initialConfig, nextDynamicConfigFileSuffix;

    Object lastSeenQuorumVerifier;

    /**
     * Parse a ZooKeeper configuration file
     * @param path the patch of the configuration file
     * @throws ConfigException error processing configuration
     */
    public void parse(String path) throws ConfigException {
        System.out.println("Reading configuration from: " + path);

        try {
            File configFile = new File(path);

            Properties cfg = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            try {
                cfg.load(in);
                configFileStr = path;
            } finally {
                in.close();
            }

            /* Read entire config file as initial configuration */
            initialConfig = new String(Files.readAllBytes(configFile.toPath()));

            parseProperties(cfg);
        } catch (IOException e) {
            throw new ConfigException("Error processing " + path, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Error processing " + path, e);
        }
        if (dynamicConfigFileStr != null) {
            try {
                Properties dynamicCfg = new Properties();
                FileInputStream inConfig = new FileInputStream(dynamicConfigFileStr);
                try {
                    dynamicCfg.load(inConfig);
                    if (dynamicCfg.getProperty("version") != null) {
                        throw new ConfigException("dynamic file shouldn't have version inside");
                    }

                    String version = getVersionFromFilename(dynamicConfigFileStr);
                    // If there isn't any version associated with the filename,
                    // the default version is 0.
                    if (version != null) {
                        dynamicCfg.setProperty("version", version);
                    }
                } finally {
                    inConfig.close();
                }
                setupQuorumPeerConfig(dynamicCfg, false);

            } catch (IOException e) {
                throw new ConfigException("Error processing " + dynamicConfigFileStr, e);
            } catch (IllegalArgumentException e) {
                throw new ConfigException("Error processing " + dynamicConfigFileStr, e);
            }
            File nextDynamicConfigFile = new File(configFileStr + nextDynamicConfigFileSuffix);
            if (nextDynamicConfigFile.exists()) {
                try {
                    Properties dynamicConfigNextCfg = new Properties();
                    FileInputStream inConfigNext = new FileInputStream(nextDynamicConfigFile);
                    try {
                        dynamicConfigNextCfg.load(inConfigNext);
                    } finally {
                        inConfigNext.close();
                    }
                    boolean isHierarchical = false;
                    for (Entry<Object, Object> entry : dynamicConfigNextCfg.entrySet()) {
                        String key = entry.getKey().toString().trim();
                        if (key.startsWith("group") || key.startsWith("weight")) {
                            isHierarchical = true;
                            break;
                        }
                    }
                    lastSeenQuorumVerifier = createQuorumVerifier(dynamicConfigNextCfg, isHierarchical);
                } catch (IOException e) {
                    System.err.println("NextQuorumVerifier is initiated to null");
                }
            }
        }
    }

    String getVersionFromFilename(String fn) {
        return "1.0";
    }

    void setupQuorumPeerConfig(Properties p, boolean b) { }

    void parseProperties(Properties p) { }

    Object createQuorumVerifier(Properties p, boolean b) {
        return null;
    }
}
