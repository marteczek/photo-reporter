package com.marteczek.photoreporter.database.entity.type;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.marteczek.photoreporter.database.entity.type.ElementStatus.*;

@StringDef({NEW, SENDING, ERROR, SENT})
@Retention(RetentionPolicy.SOURCE)
public @interface ElementStatus {
    String NEW = "new";
    String SENDING = "sending";
    String ERROR = "error";
    String SENT = "sent";
}
