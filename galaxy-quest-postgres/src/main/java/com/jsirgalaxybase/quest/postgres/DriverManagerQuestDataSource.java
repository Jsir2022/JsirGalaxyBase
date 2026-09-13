package com.jsirgalaxybase.quest.postgres;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public final class DriverManagerQuestDataSource implements DataSource {
    private final String url;
    private final String username;
    private final String password;

    public DriverManagerQuestDataSource(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url, username, password); }
    @Override public Connection getConnection(String user, String pass) throws SQLException { return DriverManager.getConnection(url, user, pass); }
    @Override public PrintWriter getLogWriter() throws SQLException { return DriverManager.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { DriverManager.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { DriverManager.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return DriverManager.getLoginTimeout(); }
    @Override public Logger getParentLogger() { return Logger.getLogger("com.jsirgalaxybase.quest.postgres"); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
}
