package com.zakpruitt.sanitycapped.audio;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.storage.DiskStore;
import org.springframework.stereotype.Component;

/** MP3 files on the data volume, named by sound id. */
@Component
public class SoundStore extends DiskStore {

    SoundStore(AppProperties props) {
        super(props.soundDir());
    }
}
