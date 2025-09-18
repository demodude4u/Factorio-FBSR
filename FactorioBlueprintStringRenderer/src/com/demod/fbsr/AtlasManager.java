package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Table;

public class AtlasManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasManager.class);

    public static int LOAD_QUEUE_MAX = 20;

    public static class LoadedAtlas {
        public final int packageId;
        public final int atlasId;
        public final BufferedImage image;

        public LoadedAtlas(int packageId, int atlasId, BufferedImage image) {
            this.packageId = packageId;
            this.atlasId = atlasId;
            this.image = image;
        }

        public static LoadedAtlas dummy() {
            return new LoadedAtlas(-1, -1, new BufferedImage(AtlasPackage.ATLAS_SIZE, AtlasPackage.ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB));
        }
    }
    
	private volatile boolean initialized = false;

    private List<AtlasPackage> packages = new ArrayList<>();
    private List<LoadedAtlas> alwaysLoaded = new ArrayList<>();
    private Deque<LoadedAtlas> loadQueue = new ArrayDeque<>();
    private LoadedAtlas[/* packageId */][/* atlasId */] loadedAtlases;

    public synchronized BufferedImage getImage(int packageId, int atlasId) {
        return null;
    }

    public int register(AtlasPackage atlasPackage) {
        packages.add(atlasPackage);
        return packages.size() - 1;
    }

    public void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
        
        loadedAtlases = new LoadedAtlas[packages.size()][];
        for (AtlasPackage atlasPackage : packages) {
            loadedAtlases[atlasPackage.getId()] = new LoadedAtlas[atlasPackage.getAtlasCount()];
            
            if (atlasPackage.isAlwaysLoaded()) {
                int[] atlasIds = IntStream.range(0, atlasPackage.getAtlasCount()).toArray();
                BufferedImage[] images = atlasPackage.readAtlases(atlasIds);
                for (int atlasId : atlasIds) {
                    BufferedImage image = images[atlasId];
                    LoadedAtlas loadedAtlas = new LoadedAtlas(atlasPackage.getId(), atlasId, image);
                    loadedAtlases[atlasPackage.getId()][atlasId] = loadedAtlas;
                    alwaysLoaded.add(loadedAtlas);
                }
            }
        }

        //Load up queue with dummy atlases to make sure memory allocation is satisfied
        while (loadQueue.size() < LOAD_QUEUE_MAX) {
            loadQueue.add(LoadedAtlas.dummy());
        }
    }

    public synchronized BufferedImage requestAtlas(int packageId, int atlasId) {
        LoadedAtlas loadedAtlas = loadedAtlases[packageId][atlasId];
        if (loadedAtlas != null) {
            return loadedAtlas.image;
        }

        loadedAtlas = loadQueue.poll();
        if (loadedAtlas.packageId != -1) {
            loadedAtlases[loadedAtlas.packageId][loadedAtlas.atlasId] = null;
        }

        AtlasPackage atlasPackage = packages.get(packageId);
        BufferedImage image = atlasPackage.readAtlases(atlasId)[0];
        loadedAtlas = new LoadedAtlas(packageId, atlasId, image);
        loadedAtlases[packageId][atlasId] = loadedAtlas;
        loadQueue.add(loadedAtlas);
        return loadedAtlas.image;
    }
}
