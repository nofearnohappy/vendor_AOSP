/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.cts;

import android.net.TrafficStats;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TrafficStatsTest extends AndroidTestCase {
    private static final String LOG_TAG = "TrafficStatsTest";

    public void testValidMobileStats() {
        // We can't assume a mobile network is even present in this test, so
        // we simply assert that a valid value is returned.

        assertTrue(TrafficStats.getMobileTxPackets() >= 0);
        assertTrue(TrafficStats.getMobileRxPackets() >= 0);
        assertTrue(TrafficStats.getMobileTxBytes() >= 0);
        assertTrue(TrafficStats.getMobileRxBytes() >= 0);
    }

    public void testValidTotalStats() {
        assertTrue(TrafficStats.getTotalTxPackets() >= 0);
        assertTrue(TrafficStats.getTotalRxPackets() >= 0);
        assertTrue(TrafficStats.getTotalTxBytes() >= 0);
        assertTrue(TrafficStats.getTotalRxBytes() >= 0);
    }

    public void testThreadStatsTag() throws Exception {
        TrafficStats.setThreadStatsTag(0xf00d);
        assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xf00d);

        final CountDownLatch latch = new CountDownLatch(1);

        new Thread("TrafficStatsTest.testThreadStatsTag") {
            @Override
            public void run() {
                assertTrue("Tag leaked", TrafficStats.getThreadStatsTag() != 0xf00d);
                TrafficStats.setThreadStatsTag(0xcafe);
                assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xcafe);
                latch.countDown();
            }
        } .start();

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Tag lost", TrafficStats.getThreadStatsTag() == 0xf00d);

        TrafficStats.clearThreadStatsTag();
        assertTrue("Tag not cleared", TrafficStats.getThreadStatsTag() != 0xf00d);
    }

    long tcpPacketToIpBytes(long packetCount, long bytes) {
        // ip header + tcp header + data.
        // Tcp header is mostly 32. Syn has different tcp options -> 40. Don't care.
        return packetCount * (20 + 32 + bytes);
    }

    private void accessOwnTrafficStats() throws IOException {
        final int ownAppUid = getContext().getApplicationInfo().uid;
        Log.d(LOG_TAG, "accesOwnTrafficStatsWithTags(): about to read qtaguid stats for own uid " + ownAppUid);

        boolean foundOwnDetailedStats = false;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader("/proc/net/xt_qtaguid/stats"));
            String line;
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                if (tokens.length > 3 && tokens[3].equals(String.valueOf(ownAppUid))) {
                    Log.d(LOG_TAG, "accessOwnTrafficStatsWithTags(): got own stats: " + line);
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
    }
}
