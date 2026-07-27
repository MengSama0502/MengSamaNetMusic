package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class QqCredentialManager {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicReference<QqCredential> CURRENT = new AtomicReference<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static volatile Path storageFile;

    private QqCredentialManager() {}

    public static synchronized void init(Path configRoot) {
        storageFile = configRoot.resolve("mengsamanetmusic").resolve("credential.json");
        load();
    }

    public static synchronized void load() {
        QqCredential loaded = null;
        Path file = storageFile;
        if (file != null && Files.isRegularFile(file)) {
            try {
                loaded = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), QqCredential.class);
                if (loaded != null && !loaded.isValid()) loaded = null;
            } catch (Exception error) {
                MengSamaNetMusic.LOGGER.warn("Ignoring unreadable QQ credential file {}", file, error);
            }
        }
        publish(loaded);
    }

    public static synchronized void save(QqCredential credential) {
        if (credential == null || !credential.isValid()) {
            clear();
            return;
        }
        Path file = storageFile;
        if (file != null) {
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(temporary, JSON.toJson(credential), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicMoveUnsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException error) {
                MengSamaNetMusic.LOGGER.error("Could not persist QQ credential", error);
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
        publish(credential);
    }

    public static synchronized void clear() {
        Path file = storageFile;
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException error) {
                MengSamaNetMusic.LOGGER.error("Could not remove QQ credential", error);
            }
        }
        publish(null);
    }

    private static void publish(QqCredential credential) {
        CURRENT.set(credential);
        REVISION.incrementAndGet();
    }

    public static QqCredential getCredential() { return CURRENT.get(); }
    public static long revision() { return REVISION.get(); }
    public static boolean hasValidCredential() { return Optional.ofNullable(CURRENT.get()).map(QqCredential::isValid).orElse(false); }
    public static String getEffectiveCookie() { return Optional.ofNullable(CURRENT.get()).map(QqCredential::toCookieString).orElse(""); }
    public static String getMusicId() { return Optional.ofNullable(CURRENT.get()).map(QqCredential::getMusicId).orElse(""); }
}
