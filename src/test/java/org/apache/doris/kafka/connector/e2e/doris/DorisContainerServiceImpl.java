/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.doris.kafka.connector.e2e.doris;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.apache.doris.kafka.connector.exception.DorisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerLoggerFactory;

public class DorisContainerServiceImpl implements DorisContainerService {
    protected static final Logger LOG = LoggerFactory.getLogger(DorisContainerServiceImpl.class);
    protected static final String DORIS_DOCKER_IMAGE = "apache/doris:doris-all-in-one-2.1.0";
    private static final String DRIVER_JAR =
            "https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.16/mysql-connector-java-8.0.16.jar";
    protected static final String JDBC_URL = "jdbc:mysql://%s:9030";
    protected static final String USERNAME = "root";
    protected static final String PASSWORD = "";
    private final GenericContainer dorisContainer;

    public DorisContainerServiceImpl() {
        dorisContainer = createDorisContainer();
    }

    public GenericContainer createDorisContainer() {
        LOG.info("Will create doris containers.");
        GenericContainer container =
                new GenericContainer<>(DORIS_DOCKER_IMAGE)
                        .withNetwork(Network.newNetwork())
                        .withNetworkAliases("DorisContainer")
                        .withPrivilegedMode(true)
                        .withLogConsumer(
                                new Slf4jLogConsumer(
                                        DockerLoggerFactory.getLogger(DORIS_DOCKER_IMAGE)))
                        .withExposedPorts(8030, 9030, 8040, 9060);

        container.setPortBindings(
                Lists.newArrayList(
                        String.format("%s:%s", "8030", "8030"),
                        String.format("%s:%s", "9030", "9030"),
                        String.format("%s:%s", "9060", "9060"),
                        String.format("%s:%s", "8040", "8040")));
        return container;
    }

    public void startContainer() {
        try {
            LOG.info("Starting doris containers.");
            // singleton doris container
            dorisContainer.start();
            initializeJdbcConnection();
            printClusterStatus();
        } catch (Exception ex) {
            LOG.error("Failed to start containers doris", ex);
            throw new DorisException("Failed to start containers doris", ex);
        }
        LOG.info("Doris container started successfully.");
    }

    @Override
    public String getInstanceHost() {
        return dorisContainer.getHost();
    }

    public void close() {
        LOG.info("Doris container is about to be close.");
        dorisContainer.close();
        LOG.info("Doris container closed successfully.");
    }

    private void initializeJdbcConnection() throws Exception {
        URLClassLoader urlClassLoader =
                new URLClassLoader(
                        new URL[] {new URL(DRIVER_JAR)},
                        DorisContainerServiceImpl.class.getClassLoader());
        LOG.info("Try to connect to Doris.");
        Thread.currentThread().setContextClassLoader(urlClassLoader);
        try (Connection connection =
                        DriverManager.getConnection(
                                String.format(JDBC_URL, dorisContainer.getHost()),
                                USERNAME,
                                PASSWORD);
                Statement statement = connection.createStatement()) {
            ResultSet resultSet;
            do {
                LOG.info("Waiting for the Backend to start successfully.");
                resultSet = statement.executeQuery("show backends");
            } while (!isBeReady(resultSet, Duration.ofSeconds(1L)));
        }
        LOG.info("Connected to Doris successfully.");
    }

    private boolean isBeReady(ResultSet rs, Duration duration) throws SQLException {
        LockSupport.parkNanos(duration.toNanos());
        if (rs.next()) {
            String isAlive = rs.getString("Alive").trim();
            String totalCap = rs.getString("TotalCapacity").trim();
            return "true".equalsIgnoreCase(isAlive) && !"0.000".equalsIgnoreCase(totalCap);
        }
        return false;
    }

    private void printClusterStatus() throws Exception {
        LOG.info("Current machine IP: {}", dorisContainer.getHost());
        echo("sh", "-c", "cat /proc/cpuinfo | grep 'cpu cores' | uniq");
        echo("sh", "-c", "free -h");
        try (Connection connection =
                        DriverManager.getConnection(
                                String.format(JDBC_URL, dorisContainer.getHost()),
                                USERNAME,
                                PASSWORD);
                Statement statement = connection.createStatement()) {
            ResultSet showFrontends = statement.executeQuery("show frontends");
            LOG.info("Frontends status: {}", convertList(showFrontends));
            ResultSet showBackends = statement.executeQuery("show backends");
            LOG.info("Backends status: {}", convertList(showBackends));
        }
    }

    private void echo(String... cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();
            is.close();
            reader.close();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Map> convertList(ResultSet rs) throws SQLException {
        List<Map> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            Map<String, Object> rowData = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                rowData.put(metaData.getColumnName(i), rs.getObject(i));
            }
            list.add(rowData);
        }
        return list;
    }
}
