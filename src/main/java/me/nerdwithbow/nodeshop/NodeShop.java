package me.nerdwithbow.nodeshop;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;

public class NodeShop extends JavaPlugin {

	private Connection connection = null;
	private Permission permission = null;
	private Economy economy = null;

	@Override
	public void onEnable() {
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdir();
		}

		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:plugins"
					+ File.separator + this.getDataFolder().getName()
					+ File.separator + "node_shop.db");

			PreparedStatement itemStmt = connection
					.prepareStatement("CREATE TABLE IF NOT EXISTS item"
							+ " (id INTEGER PRIMARY KEY,"
							+ " name TEXT NOT NULL,"
							+ " description TEXT DEFAULT 'A shop item.',"
							+ " permission TEXT NOT NULL,"
							+ " price REAL NOT NULL)");

			itemStmt.executeUpdate();
			itemStmt.close();

			PreparedStatement purchaseStmt = connection
					.prepareStatement("CREATE TABLE IF NOT EXISTS purchase"
							+ " (id INTEGER PRIMARY KEY,"
							+ " player_uuid TEXT NOT NULL,"
							+ " item_id INTEGER NOT NULL)");

			purchaseStmt.executeUpdate();
			purchaseStmt.close();

			if (!this.setupPermissions() || !this.setupEconomy()) {
				this.getLogger()
						.severe("Vault permissions or economy did not load properly - disabling plugin!");
				this.getServer().getPluginManager().disablePlugin(this);
				return;
			}

			/*
			 * ResultSet purchases =
			 * statement.executeQuery("SELECT player_uuid, item_id" +
			 * " FROM purchase");
			 * 
			 * Player player = null;
			 * 
			 * while (purchases.next()) { player =
			 * this.getServer().getPlayer(purchases.getString("player_uuid"));
			 * 
			 * if (player != null) { ResultSet item =
			 * statement.executeQuery("SELECT permission" + " FROM item" +
			 * " WHERE id = " + purchases.getInt("item_id")); item.next();
			 * this.permission.playerAdd(null, player,
			 * item.getString("permission")); } }
			 */
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			this.getLogger()
					.severe("SQLite database is not functioning properly - disabling plugin!");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		this.getCommand("nodeshop").setExecutor(
				new NodeShopCommandExecutor(this, this.connection));
	}

	@Override
	public void onDisable() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = this.getServer()
				.getServicesManager().getRegistration(Permission.class);
		if (rsp != null) {
			this.permission = rsp.getProvider();
		}

		return this.permission != null;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = this.getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp != null) {
			this.economy = rsp.getProvider();
		}

		return this.economy != null;
	}

	public PreparedStatement getPreparedStatement(String sql, String arg,
			int parameterIndex) throws SQLException {
		PreparedStatement stmt = null;

		try {
			int id = Integer.parseInt(arg);
			stmt = this.connection.prepareStatement(sql + "id = ?");
			stmt.setInt(parameterIndex, id);
		} catch (NumberFormatException ex) {
			stmt = this.connection.prepareStatement(sql + "name = ?");
			stmt.setString(parameterIndex, arg);
		}

		return stmt;
	}

	public Permission getPermissions() {
		return this.permission;
	}

	public Economy getEconomy() {
		return this.economy;
	}

	public Connection getConnection() {
		return this.connection;
	}
}
