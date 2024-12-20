package dev.mrsnowy.teleport_commands.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import dev.mrsnowy.teleport_commands.TeleportCommands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StorageManager {
    public static Path STORAGE_FOLDER;
    public static Path STORAGE_FILE;

    public static void StorageInit() {
        STORAGE_FOLDER = TeleportCommands.SAVE_DIR.resolve("TeleportCommands/");
        STORAGE_FILE = STORAGE_FOLDER.resolve("storage.json");

        try {
            if (!Files.exists(STORAGE_FOLDER)) {
                Files.createDirectories(STORAGE_FOLDER);
            }

            if (!Files.exists(STORAGE_FILE)) {
                Files.createFile(STORAGE_FILE);
            }

            // create the storage
            if (new File(String.valueOf(STORAGE_FILE)).length() == 0) {
                StorageClass root = new StorageClass();
                root.Players = new ArrayList<>();
                root.Warps = new ArrayList<>();
                StorageSaver(root);
            }

        } catch (Exception e) {
            TeleportCommands.LOGGER.error("Error while creating the storage file! Exiting! {}", e.getMessage());
            // crashing is probably better here, otherwise the whole mod will be broken
            System.exit(1);
        }
    }

    public static void StorageAdd(String UUID) throws Exception {
        StorageClass storage = StorageRetriever();

        Optional<StorageClass.Player> playerStorage = storage.Players.stream()
                .filter(player -> Objects.equals(UUID, player.UUID))
                .findFirst();

        if (playerStorage.isEmpty()) {
            StorageClass.Player newPlayer = new StorageClass.Player();

            newPlayer.UUID = UUID;
            newPlayer.DefaultHome = "";
            newPlayer.deathLocation = new StorageClass.Location();
            newPlayer.deathLocation.x = new StorageClass.Location().x;
            newPlayer.deathLocation.y = new StorageClass.Location().y;
            newPlayer.deathLocation.z = new StorageClass.Location().z;
            newPlayer.deathLocation.world = "";

            newPlayer.Homes = new ArrayList<>();

            List<StorageClass.Player> playerList = storage.Players;
            playerList.add(newPlayer);

            StorageSaver(storage);
            TeleportCommands.LOGGER.info("Player '{}' added successfully in storage!", UUID);
        } else {
            TeleportCommands.LOGGER.info("Player '{}' already exists!", UUID);
        }
    }

    public static void StorageSaver(StorageClass storage) throws Exception {
        Gson gson = new GsonBuilder().create();
        byte[] json = gson.toJson(storage).getBytes();
        Files.write(STORAGE_FILE, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static StorageClass StorageRetriever() throws Exception {
        // double check that the storage file is intact
        if (new File(String.valueOf(STORAGE_FILE)).length() == 0) {
            StorageInit();
        }
        String jsonContent = Files.readString(STORAGE_FILE);
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonContent, StorageClass.class);
    }

    public static Pair<StorageClass, List<StorageClass.NamedLocation>> getWarpStorage() throws Exception {
        StorageClass storage = StorageRetriever();
        return new Pair<>(storage, storage.Warps);
    }


    public static Pair<StorageClass, StorageClass.Player> GetPlayerStorage(String UUID) throws Exception {
        StorageClass storage = StorageRetriever();

        Optional<StorageClass.Player> playerStorage = storage.Players.stream()
                .filter(player -> Objects.equals(UUID, player.UUID))
                .findFirst();

        if (playerStorage.isEmpty()) {
            StorageAdd(UUID);

            storage = StorageRetriever();

            playerStorage = storage.Players.stream()
                    .filter(player -> Objects.equals(UUID, player.UUID))
                    .findFirst();

            if (playerStorage.isEmpty()) {
                throw new Exception("No Player found?!");
            }
        }

        return new Pair<>(storage, playerStorage.get());
    }


    public static class StorageClass {
        public List<NamedLocation> Warps;
        public List<Player> Players;

        public static class NamedLocation {
            public String name;
            public int x;
            public int y;
            public int z;
            public String world;
        }

        public static class Location {
            public int x;
            public int y;
            public int z;
            public String world;
        }

        public static class Player {
            public String UUID;
            public String DefaultHome;
            public Location deathLocation;
            public List<NamedLocation> Homes;
        }
    }
}
