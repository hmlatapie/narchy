/*
 * Copyright 2012 Jonathan Leahey
 * 
 * This file is part of Minicraft
 * 
 * Minicraft is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * Minicraft is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Minicraft. If not, see http://www.gnu.org/licenses/.
 */

package nars.experiment.minicraft.side;

public class MainMenu {
	
	/* menu sprites */
	private static final Sprite menu_bgTile = SpriteStore.get().getSprite("sprites/tiles/dirt.png");
	private static final Sprite menu_logo = SpriteStore.get().getSprite("sprites/menus/title.png");
	private static final Sprite menu_newUp = SpriteStore.get().getSprite("sprites/menus/new_up.png");
	private static final Sprite menu_newDown = SpriteStore.get().getSprite("sprites/menus/new_down.png");
	private static final Sprite menu_loadUp = SpriteStore.get().getSprite("sprites/menus/load_up.png");
	private static final Sprite menu_loadDown = SpriteStore.get().getSprite("sprites/menus/load_down.png");
	private static final Sprite menu_quitUp = SpriteStore.get().getSprite("sprites/menus/quit_up.png");
	private static final Sprite menu_quitDown = SpriteStore.get().getSprite("sprites/menus/quit_down.png");
	private static final Sprite menu_miniUp = SpriteStore.get().getSprite("sprites/menus/mini_up.png");
	private static final Sprite menu_mediumUp = SpriteStore.get().getSprite("sprites/menus/med_up.png");
	private static final Sprite menu_bigUp = SpriteStore.get().getSprite("sprites/menus/big_up.png");
	private static final Sprite menu_miniDown = SpriteStore.get().getSprite("sprites/menus/mini_down.png");
	private static final Sprite menu_mediumDown = SpriteStore.get().getSprite("sprites/menus/med_down.png");
	private static final Sprite menu_bigDown = SpriteStore.get().getSprite("sprites/menus/big_down.png");
	private static final Sprite menu_tag = SpriteStore.get().getSprite("sprites/menus/tag.png");
	private static final int menu_miniWidth = 256;
	private static final int menu_mediumWidth = 512;
	private static final int menu_bigWidth = 1024;
	
	private boolean newGame;
	private final SideScrollMinicraft game;
	
	public MainMenu(SideScrollMinicraft g) {
		this.game = g;
	}

	public void draw(GraphicsHandler g) {
		SideScrollMinicraft.drawTileBackground(g, menu_bgTile, 32);
		SideScrollMinicraft.drawCenteredX(g, menu_logo, 70, 397, 50);
		float tagScale = ((float) Math.abs((game.ticksRunning % 100) - 50)) / 50 + 1;
		menu_tag.draw(g, 450, 70, (int) (60 * tagScale), (int) (37 * tagScale));

		if (newGame) {
			drawNewMenu(g);
		} else {
			drawStartMenu(g);
		}
	}
	
	private void drawStartMenu(GraphicsHandler g) {
		SideScrollMinicraft.drawCenteredX(g, menu_newUp, 150, 160, 64);
		SideScrollMinicraft.drawCenteredX(g, menu_loadUp, 250, 160, 64);
		SideScrollMinicraft.drawCenteredX(g, menu_quitUp, 350, 160, 64);
		int mouseY = game.screenMousePos.y;
		// TODO: use the mouse x-value as well
		if (mouseY >= 350 && mouseY <= 414) {
			SideScrollMinicraft.drawCenteredX(g, menu_quitDown, 350, 160, 64);
		} else if (mouseY >= 250 && mouseY <= 314) {
			SideScrollMinicraft.drawCenteredX(g, menu_loadDown, 250, 160, 64);
		} else if (mouseY >= 150 && mouseY <= 214) {
			SideScrollMinicraft.drawCenteredX(g, menu_newDown, 150, 160, 64);
		}
		if (!game.leftClick) {
			return;
		}
		game.leftClick = false;
		if (mouseY >= 350 && mouseY <= 414) {
			SideScrollMinicraft.quit();  // "quit" button
		} else if (mouseY >= 250 && mouseY <= 314) {
			game.startGame(true, menu_mediumWidth);  // "load" button
		} else if (mouseY >= 150 && mouseY <= 214) {
			newGame = true;  // "new" button
		}
	}
	
	private void drawNewMenu(GraphicsHandler g) {
		SideScrollMinicraft.drawCenteredX(g, menu_miniUp, 150, 160, 64);
		SideScrollMinicraft.drawCenteredX(g, menu_mediumUp, 250, 160, 64);
		SideScrollMinicraft.drawCenteredX(g, menu_bigUp, 350, 160, 64);
		int mouseY = game.screenMousePos.y;
		// TODO: use the mouse x-value as well
		if (mouseY >= 350 && mouseY <= 414) {
			SideScrollMinicraft.drawCenteredX(g, menu_bigDown, 350, 160, 64);
		} else if (mouseY >= 250 && mouseY <= 314) {
			SideScrollMinicraft.drawCenteredX(g, menu_mediumDown, 250, 160, 64);
		} else if (mouseY >= 150 && mouseY <= 214) {
			SideScrollMinicraft.drawCenteredX(g, menu_miniDown, 150, 160, 64);
		}
		if (!game.leftClick) {
			return;
		}
		game.leftClick = false;
		newGame = false;
		if (mouseY >= 350 && mouseY <= 414) {
			game.startGame(false, menu_bigWidth);
		} else if (mouseY >= 250 && mouseY <= 314) {
			game.startGame(false, menu_mediumWidth);
		} else if (mouseY >= 150 && mouseY <= 214) {
			game.startGame(false, menu_miniWidth);
		}
	}
}
