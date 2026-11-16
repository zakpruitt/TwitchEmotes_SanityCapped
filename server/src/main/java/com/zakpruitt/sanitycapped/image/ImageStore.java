package com.zakpruitt.sanitycapped.image;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.storage.DiskStore;
import org.springframework.stereotype.Component;

/** Image files on the data volume, named by emote id. */
@Component
public class ImageStore extends DiskStore {

    ImageStore(AppProperties props) {
        super(props.imageDir());
    }
}
