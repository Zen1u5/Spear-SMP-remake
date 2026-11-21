# SpearSMP 2.0.0
Paper plugin source for Java 21 / Paper 1.21.11.

## Build
Push this folder's contents to a new GitHub repository on `main`. GitHub Actions builds `target/SpearSMP-2.0.0.jar` and publishes it as the `SpearSMP-plugin-2.0.0` artifact.

## Install
Stop the server, back up the old plugin and its data, remove the old SpearSMP JAR, upload the newly built JAR to `plugins/`, then start the server.

## Commands
- `/spear start`
- `/spear stats`
- `/spear give <player> <tier>` (operator permission `spearsmp.admin`)
- `/spear reload`
- `/spear help`

Tiers: WOODEN, STONE, COAL, EMERALD, COPPER, REDSTONE, IRON, LAPIS, GOLD, DIAMOND, NETHERITE, GOD.

Passive effects apply while a SpearSMP spear is in the inventory. Player kills progress the held/inventory spear. Right-click is cancelled to prevent vanilla trident throwing. No fake-player `/player` command is included.

IMPORTANT: The resource pack is a separate file. Upload it to a hosting service and configure the resulting direct download URL and SHA-1 in `server.properties`. Verify pack format/model rendering on your exact server/client version before requiring it.
