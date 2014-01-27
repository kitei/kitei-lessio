/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitei.testing.lessio.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import org.junit.Test;
import org.kitei.testing.lessio.LessIOException;

public class TestAllowNetworkListenDenied
{
    @Test(expected = LessIOException.class)
    public void testNetworkListen() throws Exception
    {
        try (final ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(0), 5);
            serverSocket.setSoTimeout(1000);

            final String testString = "This is a test string\n" + UUID.randomUUID();

            final int serverPort = serverSocket.getLocalPort();

            final Runnable r = new Runnable() {

                @Override
                public void run()
                {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(serverPort));
                        try (final BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
                            wr.append(testString);
                            wr.flush();
                        }
                    }
                    catch (IOException ioe) {
                        fail("Exception: " + ioe.getMessage());
                    }
                }

            };

            ScheduledExecutorService es = Executors.newScheduledThreadPool(5);
            es.schedule(r, 100, TimeUnit.MILLISECONDS);

            final Socket socket = serverSocket.accept();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String data = Joiner.on("\n").join(CharStreams.readLines(in));
                assertNotNull("no data received?", data);
                assertEquals("Bad data received", testString, data);
            }

            es.shutdown();
        }
    }
}

