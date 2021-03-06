package org.apache.pig.backend.hadoop.executionengine.mapReduceLayer;

import java.io.IOException;

import org.joda.time.DateTime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapred.JobConf;
import org.apache.pig.backend.hadoop.DateTimeWritable;
import org.apache.pig.impl.io.NullableDateTimeWritable;
import org.apache.pig.impl.util.ObjectSerializer;

public class PigDateTimeRawComparator extends WritableComparator implements
        Configurable {

    private final Log mLog = LogFactory.getLog(getClass());
    private boolean[] mAsc;
    private DateTimeWritable.Comparator mWrappedComp;

    public PigDateTimeRawComparator() {
        super(NullableDateTimeWritable.class);
        mWrappedComp = new DateTimeWritable.Comparator();
    }

    public void setConf(Configuration conf) {
        if (!(conf instanceof JobConf)) {
            mLog.warn("Expected jobconf in setConf, got "
                    + conf.getClass().getName());
            return;
        }
        JobConf jconf = (JobConf) conf;
        try {
            mAsc = (boolean[]) ObjectSerializer.deserialize(jconf
                    .get("pig.sortOrder"));
        } catch (IOException ioe) {
            mLog.error("Unable to deserialize pig.sortOrder "
                    + ioe.getMessage());
            throw new RuntimeException(ioe);
        }
        if (mAsc == null) {
            mAsc = new boolean[1];
            mAsc[0] = true;
        }
    }

    public Configuration getConf() {
        return null;
    }

    /**
     * Compare two NullableIntWritables as raw bytes. If neither are null, then
     * IntWritable.compare() is used. If both are null then the indices are
     * compared. Otherwise the null one is defined to be less.
     */
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
        int rc = 0;

        // If either are null, handle differently.
        if (b1[s1] == 0 && b2[s2] == 0) {
            rc = mWrappedComp.compare(b1, s1 + 1, l1 - 2, b2, s2 + 1, l2 - 2);
        } else {
            // For sorting purposes two nulls are equal.
            if (b1[s1] != 0 && b2[s2] != 0)
                rc = 0;
            else if (b1[s1] != 0)
                rc = -1;
            else
                rc = 1;
        }
        if (!mAsc[0])
            rc *= -1;
        return rc;
    }

    public int compare(Object o1, Object o2) {
        NullableDateTimeWritable ndtw1 = (NullableDateTimeWritable) o1;
        NullableDateTimeWritable ndtw2 = (NullableDateTimeWritable) o2;
        int rc = 0;

        // If either are null, handle differently.
        if (!ndtw1.isNull() && !ndtw2.isNull()) {
            rc = ((DateTime) ndtw1.getValueAsPigType())
                    .compareTo((DateTime) ndtw2.getValueAsPigType());
        } else {
            // For sorting purposes two nulls are equal.
            if (ndtw1.isNull() && ndtw2.isNull())
                rc = 0;
            else if (ndtw1.isNull())
                rc = -1;
            else
                rc = 1;
        }
        if (!mAsc[0])
            rc *= -1;
        return rc;
    }

}
