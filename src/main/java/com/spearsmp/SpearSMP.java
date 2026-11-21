
package com.spearsmp;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public final class SpearSMP extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey idKey, tierKey;
    private static final String[] N={"WOODEN","STONE","COAL","EMERALD","COPPER","REDSTONE","IRON","LAPIS","GOLD","DIAMOND","NETHERITE","GOD"};
    private static final int[] NEED={1,1,1,2,2,2,2,2,3,3,3};
    private static final ChatColor[] C={ChatColor.GOLD,ChatColor.GRAY,ChatColor.DARK_GRAY,ChatColor.GREEN,ChatColor.GOLD,ChatColor.RED,ChatColor.WHITE,ChatColor.BLUE,ChatColor.YELLOW,ChatColor.AQUA,ChatColor.DARK_PURPLE,ChatColor.LIGHT_PURPLE};
    private static final PotionEffectType[] EFFECT={null,PotionEffectType.RESISTANCE,PotionEffectType.FIRE_RESISTANCE,PotionEffectType.HERO_OF_THE_VILLAGE,PotionEffectType.HASTE,PotionEffectType.SPEED,PotionEffectType.STRENGTH,PotionEffectType.SPEED,PotionEffectType.STRENGTH,PotionEffectType.RESISTANCE,PotionEffectType.STRENGTH,PotionEffectType.STRENGTH};
    private static final int[] LEVEL={0,1,1,5,1,1,1,2,2,2,3,4};

    @Override
    public void onEnable(){
        saveDefaultConfig();
        idKey=new NamespacedKey(this,"spearsmp_item");
        tierKey=new NamespacedKey(this,"spearsmp_tier");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("spear")).setExecutor(this);
        long ticks=Math.max(20,getConfig().getLong("passive-check-ticks",40));
        getServer().getScheduler().runTaskTimer(this,this::applyPassives,20,ticks);
        getLogger().info("SpearSMP 2.0.0 enabled");
    }

    private ItemStack create(int t,int progress){
        ItemStack it=new ItemStack(Material.TRIDENT);
        ItemMeta m=it.getItemMeta();
        m.setDisplayName(C[t].toString()+ChatColor.BOLD+N[t]+" SPEAR");
        m.setCustomModelData(1201+t);
        m.setUnbreakable(true);
        m.getPersistentDataContainer().set(idKey,PersistentDataType.BYTE,(byte)1);
        m.getPersistentDataContainer().set(tierKey,PersistentDataType.INTEGER,t);
        List<String> lore=new ArrayList<>();
        lore.add(ChatColor.GRAY+"A relic of the "+N[t].toLowerCase(Locale.ROOT)+" tier.");
        lore.add("");
        lore.add(ChatColor.GRAY+"Progress: "+ChatColor.WHITE+(t>=11?"MAX":progress+"/"+NEED[t]));
        if(t<11)lore.add(ChatColor.DARK_GRAY+"Next: "+C[t+1].toString()+ChatColor.BOLD+N[t+1]+" SPEAR");
        PotionEffectType e=EFFECT[t];
        if(e!=null)lore.add(ChatColor.GRAY+"Passive: "+ChatColor.WHITE+pretty(e)+" "+roman(LEVEL[t]));
        m.setLore(lore);
        m.addEnchant(Enchantment.UNBREAKING,3,true);
        if(t>=4)m.addEnchant(Enchantment.SHARPNESS,Math.min(5,1+(t-4)/2),true);
        if(t>=7)m.addEnchant(Enchantment.LOOTING,Math.min(3,1+(t-7)/2),true);
        if(t>=9)m.addEnchant(Enchantment.FIRE_ASPECT,1,true);
        it.setItemMeta(m);
        return it;
    }

    private String pretty(PotionEffectType e){
        return e.getKey().getKey().replace('_',' ').toLowerCase(Locale.ROOT).replaceAll("\\b\\w","$0");
    }

    private String roman(int n){
        return switch(n){
            case 1->"I";
            case 2->"II";
            case 3->"III";
            case 4->"IV";
            case 5->"V";
            default->"";
        };
    }

    private boolean isSpear(ItemStack i){
        return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(idKey,PersistentDataType.BYTE);
    }

    private int tier(ItemStack i){
        return i.getItemMeta().getPersistentDataContainer().getOrDefault(tierKey,PersistentDataType.INTEGER,0);
    }

    private ItemStack find(Player p){
        for(ItemStack i:p.getInventory().getContents())if(isSpear(i))return i;
        if(isSpear(p.getInventory().getItemInOffHand()))return p.getInventory().getItemInOffHand();
        return null;
    }

    private int progress(UUID u){
        return getConfig().getInt("players."+u+".progress",0);
    }

    private void setProgress(UUID u,int n){
        getConfig().set("players."+u+".progress",n);
        getConfig().set("players."+u+".kills",getConfig().getInt("players."+u+".kills",0)+1);
        saveConfig();
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e){
        Player p=e.getEntity().getKiller();
        if(p==null)return;
        ItemStack s=find(p);
        if(s==null)return;
        int t=tier(s);
        if(t>=11)return;
        int n=progress(p.getUniqueId())+1;
        setProgress(p.getUniqueId(),n);
        if(n>=NEED[t]){
            int slot=p.getInventory().first(s);
            ItemStack next=create(t+1,0);
            if(slot>=0)p.getInventory().setItem(slot,next);
            else p.getInventory().setItemInOffHand(next);
            getConfig().set("players."+p.getUniqueId()+".progress",0);
            saveConfig();
            p.sendMessage(ChatColor.GREEN+"✦ Upgraded! "+C[t].toString()+ChatColor.BOLD+N[t]+" SPEAR "+ChatColor.GRAY+"→ "+C[t+1].toString()+ChatColor.BOLD+N[t+1]+" SPEAR");
            p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,1f,1f);
        }else{
            updateLore(s,n);
            p.sendMessage(C[t].toString()+ChatColor.BOLD+N[t]+" SPEAR "+ChatColor.GRAY+"Progress: "+ChatColor.WHITE+n+"/"+NEED[t]);
        }
    }

    private void updateLore(ItemStack s,int n){
        ItemMeta m=s.getItemMeta();
        List<String> l=m.getLore();
        if(l!=null&&l.size()>2)l.set(2,ChatColor.GRAY+"Progress: "+ChatColor.WHITE+n+"/"+NEED[tier(s)]);
        m.setLore(l);
        s.setItemMeta(m);
    }

    private void applyPassives(){
        for(Player p:Bukkit.getOnlinePlayers()){
            ItemStack s=find(p);
            if(s==null)continue;
            int t=tier(s);
            PotionEffectType type=EFFECT[t];
            if(type!=null)p.addPotionEffect(new PotionEffect(type,80,LEVEL[t]-1,false,false,true));
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e){
        if(isSpear(e.getItem())&&e.getAction().isRightClick()){
            e.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(a.length==0||a[0].equalsIgnoreCase("help")){
            s.sendMessage(ChatColor.LIGHT_PURPLE+"SpearSMP: /spear start, stats, give <player> <tier>, reload");
            return true;
        }
        switch(a[0].toLowerCase(Locale.ROOT)){
            case "start"->{
                if(!(s instanceof Player p)){
                    s.sendMessage("Players only.");
                    return true;
                }
                p.getInventory().addItem(create(0,0));
                p.sendMessage(ChatColor.GREEN+"You received the "+C[0].toString()+ChatColor.BOLD+N[0]+" SPEAR");
            }
            case "stats"->{
                if(!(s instanceof Player p)){
                    s.sendMessage("Players only.");
                    return true;
                }
                ItemStack i=find(p);
                if(i==null)s.sendMessage(ChatColor.YELLOW+"You don't have a SpearSMP spear in your inventory.");
                else{
                    int t=tier(i);
                    s.sendMessage(C[t].toString()+ChatColor.BOLD+N[t]+" SPEAR "+ChatColor.GRAY+"Progress: "+progress(p.getUniqueId())+"/"+(t==11?"MAX":NEED[t]));
                }
            }
            case "give"->{
                if(!s.hasPermission("spearsmp.admin")||a.length<3){
                    s.sendMessage("Usage: /spear give <player> <tier>");
                    return true;
                }
                Player p=Bukkit.getPlayerExact(a[1]);
                int t=-1;
                for(int i=0;i<N.length;i++)if(N[i].equalsIgnoreCase(a[2]))t=i;
                if(p==null||t<0){
                    s.sendMessage("Player or tier not found. Tiers: "+String.join(", ",N));
                    return true;
                }
                p.getInventory().addItem(create(t,0));
                s.sendMessage("Gave "+N[t]+" spear to "+p.getName());
            }
            case "reload"->{
                if(!s.hasPermission("spearsmp.admin")){
                    s.sendMessage("No permission.");
                    return true;
                }
                reloadConfig();
                s.sendMessage(ChatColor.GREEN+"SpearSMP config reloaded.");
            }
            default->s.sendMessage("Unknown command. Use /spear help.");
        }
        return true;
    }
}
