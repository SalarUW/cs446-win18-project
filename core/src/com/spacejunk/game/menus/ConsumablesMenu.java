package com.spacejunk.game.menus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spacejunk.game.GameScreen;
import com.spacejunk.game.SpaceJunk;
import com.spacejunk.game.consumables.Consumable;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by vidxyz on 2/9/18.
 */

public class ConsumablesMenu {

    // Max items you can hold at once in your inventory
    private static final int MAX_CONSUMABLES = 4;

    private Texture[] inventoryElementTextures;
    private Texture inventoryListTexture;

    private SpaceJunk currentGame;

    public ConsumablesMenu(SpaceJunk currentGame) {

        this.currentGame = currentGame;

        inventoryElementTextures = new Texture[MAX_CONSUMABLES];

        inventoryListTexture = this.currentGame.getManager().get("inventory_list.png");
    }

    public void render(SpriteBatch canvas) {

        renderInventoryList(canvas);
        renderInventoryElements(canvas);
    }

    private void renderInventoryList(SpriteBatch canvas) {

        // Quick hack to set transparency of consumables menu
        canvas.setColor(1, 1, 1, 0.5f);
        canvas.draw(inventoryListTexture, currentGame.getxMax() - GameScreen.getScaledTextureWidth(inventoryListTexture), 0,
                GameScreen.getScaledTextureWidth(inventoryListTexture),
                GameScreen.getScaledTextureHeight(inventoryListTexture));
        canvas.setColor(1, 1, 1, 1);

    }

    private void renderInventoryElements(SpriteBatch canvas) {
        ArrayList<Consumable> inventoryObjects = this.currentGame.getLevel().getInventoryObjects();
        Set<Consumable.CONSUMABLES> inventory = this.currentGame.getLevel().getInventory();

        for(int i = 0; i < MAX_CONSUMABLES; i++) {
            if(inventoryObjects.get(i) != null && inventory.contains(inventoryObjects.get(i).getType())) {

                // there are a lot of magic numbers here
                canvas.draw(inventoryObjects.get(i).getConsumableTextureSmall(),
                        (this.currentGame.getxMax() - ((i + 1) * GameScreen.getScaledTextureWidth(inventoryListTexture) / 4) + (12 - (i * 2))),
                        GameScreen.getScaledTextureHeight(inventoryListTexture) / 7,  GameScreen.getScaledTextureWidth(inventoryObjects.get(i).getConsumableTextureSmall()),
                        GameScreen.getScaledTextureHeight(inventoryObjects.get(i).getConsumableTextureSmall()));
            }
        }
    }

    public int getInventoryWidth() {
        return GameScreen.getScaledTextureWidth(inventoryListTexture);
    }

    public int getInventoryHeight() {
        return GameScreen.getScaledTextureHeight(inventoryListTexture);

    }
}
