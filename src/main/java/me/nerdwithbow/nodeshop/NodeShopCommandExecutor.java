package me.nerdwithbow.nodeshop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NodeShopCommandExecutor implements CommandExecutor {

	private final NodeShop plugin;
	private final Connection connection;

	public NodeShopCommandExecutor(NodeShop plugin, Connection connection) {
		this.plugin = plugin;
		this.connection = connection;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (args.length == 0) {
			// usage
			return true;
		}

		try {

			if ((args[0].equalsIgnoreCase("list") || args[0]
					.equalsIgnoreCase("l")) && args.length == 1) {
				PreparedStatement itemsStmt = this.connection
						.prepareStatement("SELECT id, name, permission, price FROM item");
				ResultSet items = itemsStmt.executeQuery();

				if (!items.next()) {
					sender.sendMessage(ChatColor.GOLD
							+ "There are currently no items available.");
					itemsStmt.close();
					return true;
				}

				StringBuilder itemsBuilder = new StringBuilder(ChatColor.GOLD
						+ "Available shop items:");

				do {
					itemsBuilder.append("\n#").append((items.getInt("id")))
							.append(" ").append(items.getString("name"))
							.append("\n - Price: ")
							.append(items.getFloat("price"))
							.append(", Permission: ")
							.append(items.getString("permission"));
				} while (items.next());

				items.close();
				itemsStmt.close();

				sender.sendMessage(itemsBuilder.toString());

				return true;
			} else if ((args[0].equalsIgnoreCase("info") || args[0]
					.equalsIgnoreCase("i")) && args.length == 2) {
				String sql = "SELECT name, description" + " FROM item"
						+ " WHERE ";

				PreparedStatement stmt = this.plugin.getPreparedStatement(sql,
						args[1], 1);
				ResultSet item = stmt.executeQuery();

				if (!item.next()) {
					sender.sendMessage(ChatColor.RED + "Item not found.");
					return true;
				}

				sender.sendMessage(ChatColor.GOLD + "Name: "
						+ item.getString("name") + "\nDescription: "
						+ item.getString("description"));

				item.close();
				stmt.close();

				return true;
			} else if ((args[0].equalsIgnoreCase("buy") || args[0]
					.equalsIgnoreCase("b")) && args.length == 2) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.RED
							+ "You must be a player to purchase an item.");
					return true;
				}

				Player player = (Player) sender;
				
				PreparedStatement itemStmt = this.plugin.getPreparedStatement(
						"SELECT id, permission, price" + " FROM item"
								+ " WHERE ", args[1], 1);
				ResultSet item = itemStmt.executeQuery();

				if (!item.next()) {
					sender.sendMessage(ChatColor.RED + "Item not found.");
					item.close();
					itemStmt.close();
					return true;
				}
				
				String permission = item.getString("permission");
				
				if (this.plugin.getPermissions().playerHas(player, permission)) {
					sender.sendMessage(ChatColor.RED + "You already have that item's permission.");
					item.close();
					itemStmt.close();
					return true;
				}

				PreparedStatement purchaseStmt = this.connection
						.prepareStatement("INSERT INTO purchase (player_uuid, item_id)"
								+ "VALUES (?, ?)");
				purchaseStmt.setString(1, player.getUniqueId().toString());
				purchaseStmt.setInt(2, item.getInt("id"));
				purchaseStmt.executeUpdate();

				// give permissions, take away money
				this.plugin.getPermissions().playerAdd(null, player,
						permission);
				this.plugin.getEconomy().depositPlayer(player,
						item.getDouble("price"));

				sender.sendMessage(ChatColor.GOLD
						+ "Your item has been purchased.");

				purchaseStmt.close();
				item.close();
				itemStmt.close();

				return true;
			} else if ((args[0].equalsIgnoreCase("add")
					|| args[0].equalsIgnoreCase("new") || args[0]
						.equalsIgnoreCase("a")) && args.length > 4) {

				double price;
				try {
					price = Double.parseDouble(args[3]);
				} catch (NumberFormatException ex) {
					sender.sendMessage(ChatColor.RED
							+ "The price must be a number.");
					return true;
				}

				StringBuilder description = new StringBuilder();

				for (int i = 4; i < args.length; i++) {
					description.append(" ").append(args[i]);
				}

				PreparedStatement stmt = this.connection
						.prepareStatement("INSERT INTO item (name, permission, price, description) VALUES (?, ?, ?, ?)");
				stmt.setString(1, args[1]);
				stmt.setString(2, args[2]);
				stmt.setDouble(3, price);
				stmt.setString(4, description.toString().trim());

				stmt.executeUpdate();
				stmt.close();

				sender.sendMessage(ChatColor.GOLD + "Your item has been added.");

				return true;
			} else if ((args[0].equalsIgnoreCase("delete")
					|| args[0].equalsIgnoreCase("remove") || args[0]
						.equalsIgnoreCase("d")) && args.length == 2) {
				PreparedStatement stmt = this.plugin.getPreparedStatement(
						"DELETE FROM item WHERE ", args[1], 1);
				int rowCount = stmt.executeUpdate();

				if (rowCount == 0) {
					sender.sendMessage(ChatColor.RED + "Item not found.");
					return true;
				}

				stmt.close();

				sender.sendMessage(ChatColor.GOLD + "Item deleted.");
				return true;
			} else if ((args[0].equalsIgnoreCase("edit") || args[0]
					.equalsIgnoreCase("e")) && args.length > 3) {
				PreparedStatement stmt = this.plugin.getPreparedStatement(
						"UPDATE item SET " + args[2] + " = ? WHERE ", args[1], 2);

				if (args[2].equals("name") || args[2].equals("permission")) {
					stmt.setString(1, args[3]);
				} else if (args[2].equals("description")) {
					StringBuilder description = new StringBuilder();

					for (int i = 3; i < args.length; i++) {
						description.append(" ").append(args[i]);
					}
					
					stmt.setString(1, description.toString().trim());
				} else if (args[2].equals("price")) {
					try {
						stmt.setDouble(1, Double.parseDouble(args[3]));
					} catch (NumberFormatException ex) {
						sender.sendMessage(ChatColor.RED
								+ "The price must be a number.");
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Unknown column.");
					return true;
				}

				/*
				 * if (args[2].equals("name") || args[2].equals("description")
				 * || args[2].equals("permission")) { value = "'" + args[3] +
				 * "'"; } else if (args[2].equals("price")) { try {
				 * Double.parseDouble(args[3]); value = args[3]; } catch
				 * (NumberFormatException ex) { sender.sendMessage(ChatColor.RED
				 * + "The price must be a number."); return true; } } else {
				 * sender.sendMessage(ChatColor.RED + "Unknown column."); return
				 * true; }
				 * 
				 * String sql = "UPDATE item" + " SET " + args[2] + " = " +
				 * value + " WHERE ";
				 * 
				 * int rowCount = statement.executeUpdate(this.util
				 * .getCompletedSql(sql, args[1]));
				 */

				int rowCount = stmt.executeUpdate();

				if (rowCount == 0) {
					sender.sendMessage(ChatColor.RED + "Item not found.");
					return true;
				}

				stmt.close();

				sender.sendMessage(ChatColor.GOLD + "Item updated.");
				return true;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			sender.sendMessage(ChatColor.RED
					+ "An SQL related error has occured. Please notify a staff member.");
		}

		return false;
	}
}
