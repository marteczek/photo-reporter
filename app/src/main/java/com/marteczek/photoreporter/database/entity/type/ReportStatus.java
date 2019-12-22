package com.marteczek.photoreporter.database.entity.type;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.marteczek.photoreporter.database.entity.type.ReportStatus.*;

@StringDef({NEW, PENDING, SENT, PICTURE_SENDING_FAILURE, PICTURE_SENDING_CANCELLED, POST_CREATED, PUBLISHED})
@Retention(RetentionPolicy.SOURCE)
public @interface ReportStatus {
    String NEW = "new";
    String PENDING = "pending";
    String SENT = "sent";
    String PICTURE_SENDING_FAILURE = "picture_sending_failure";
    String PICTURE_SENDING_CANCELLED = "picture_sending_cancelled";
    String POST_CREATED = "post_created";
    String PUBLISHED = "published";
}
